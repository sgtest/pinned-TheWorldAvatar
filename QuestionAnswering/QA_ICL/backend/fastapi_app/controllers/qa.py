from collections import defaultdict
from functools import cache
import logging
from typing import Annotated

from fastapi import Depends

from model.structured_answer import (
    ChemicalStructureData,
    QARequestArtifact,
)
from model.web.qa import QAResponse, QAResponseMetadata
from services.sem_parse.retrieve_context import (
    Nlq2DataReqContextRetriever,
    get_nlq2datareq_contextRetriever,
)
from services.rewrite_nlq import NlqRewriter, get_nlq_rewriter
from services.link_entity import CentralEntityLinker, get_entity_store
from services.stores.qa_artifact_store import (
    QARequestArtifactStore,
    get_qaReq_artifactStore,
)
from services.execute_data_req import DataReqExecutor, get_dataReq_executor
from services.sem_parse.translate_nlq import (
    Nlq2DataReqLLMCaller,
    get_nlq2datareq_llmCaller,
)
from services.mol_vis.vis_data_store import VisualisationDataStore, get_visData_store


logger = logging.getLogger(__name__)


class DataSupporter:
    def __init__(
        self,
        nlq_rewriter: NlqRewriter,
        context_retriever: Nlq2DataReqContextRetriever,
        llm_caller: Nlq2DataReqLLMCaller,
        entity_store: CentralEntityLinker,
        executor: DataReqExecutor,
        artifact_store: QARequestArtifactStore,
        vis_data_store: VisualisationDataStore,
    ):
        self.nlq_rewriter = nlq_rewriter
        self.context_retriever = context_retriever
        self.llm_caller = llm_caller
        self.entity_store = entity_store
        self.executor = executor
        self.artifact_store = artifact_store
        self.vis_data_store = vis_data_store

    def query(self, query: str):
        logger.info("Input query: " + query)

        logger.info("Rewriting input query...")
        rewritten_query = self.nlq_rewriter.rewrite(question=query)
        logger.info("Rewritten query: " + rewritten_query)

        logger.info("Retrieving translation context...")
        translation_context = self.context_retriever.retrieve(nlq=rewritten_query)
        logger.info("Translation context: " + str(translation_context))

        logger.info("Translating input question into data request...")
        data_req = self.llm_caller.forward(
            nlq=rewritten_query, translation_context=translation_context
        )
        logger.info(f"Predicted data request: {data_req}")

        if data_req is None:
            data_artifact = None
            var2iris = dict()
            vis_var2structs: dict[str, list[ChemicalStructureData]] = dict()
            data = list()
        else:
            logger.info(
                f"Performing entity linking for bindings: {data_req.entity_bindings}"
            )
            var2cls = data_req.var2cls or None
            var2iris = {
                var: list(
                    set(
                        [
                            iri
                            for val in values
                            for iri in self.entity_store.link(
                                cls=var2cls and var2cls.get(var),
                                text=val if isinstance(val, str) else None,
                                identifier=val if isinstance(val, dict) else dict(),
                            )  # TODO: handle when no IRIs are returned
                        ]
                    )
                )
                for var, values in data_req.entity_bindings.items()
            }
            logger.info(f"Linked IRIs: {var2iris}")

            logger.info("Executing data request...")
            data, data_artifact, vis_var2iris = self.executor.exec(
                var2cls=data_req.var2cls,
                entity_bindings=var2iris,
                const_bindings=data_req.const_bindings,
                req_form=data_req.req_form,
                vis_vars=data_req.visualise,
            )
            logger.info("Done")

            logger.info("Retrieving visualisation data...")
            clses = [
                data_req.var2cls.get(var)
                for var in data_req.visualise
                for _ in vis_var2iris.get(var, [])
            ]
            iris = [
                iri for var in data_req.visualise for iri in vis_var2iris.get(var, [])
            ]
            chem_struct_data = self.vis_data_store.get(cls=clses, iris=iris)

            iri2var = {iri: var for var, iris in vis_var2iris.items() for iri in iris}
            vis_var2structs = defaultdict(list)
            for iri, datum in zip(iris, chem_struct_data):
                if iri not in iri2var or not datum:
                    continue
                vis_var2structs[iri2var[iri]].append(datum)
            logger.info("Done")

        logger.info("Saving QA request artifact...")
        request_id = self.artifact_store.save(
            QARequestArtifact(
                nlq=query,
                nlq_rewritten=rewritten_query if rewritten_query != query else None,
                data_req=data_req,
                data=data_artifact,
            )
        )
        logger.info("Done")

        return QAResponse(
            request_id=request_id,
            metadata=QAResponseMetadata(
                rewritten_question=(
                    rewritten_query if rewritten_query != query else None
                ),
                translation_context=translation_context,
                data_request=data_req,
                linked_variables=var2iris,
            ),
            data=data,
            visualisation=vis_var2structs,
        )


@cache
def get_data_supporter(
    nlq_rewriter: Annotated[NlqRewriter, Depends(get_nlq_rewriter)],
    context_retriever: Annotated[
        Nlq2DataReqContextRetriever, Depends(get_nlq2datareq_contextRetriever)
    ],
    llm_caller: Annotated[Nlq2DataReqLLMCaller, Depends(get_nlq2datareq_llmCaller)],
    entity_store: Annotated[CentralEntityLinker, Depends(get_entity_store)],
    executor: Annotated[DataReqExecutor, Depends(get_dataReq_executor)],
    artifact_store: Annotated[QARequestArtifactStore, Depends(get_qaReq_artifactStore)],
    vis_data_store: Annotated[VisualisationDataStore, Depends(get_visData_store)],
):
    return DataSupporter(
        nlq_rewriter=nlq_rewriter,
        context_retriever=context_retriever,
        llm_caller=llm_caller,
        entity_store=entity_store,
        executor=executor,
        artifact_store=artifact_store,
        vis_data_store=vis_data_store,
    )