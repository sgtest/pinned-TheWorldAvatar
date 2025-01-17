import Image from "next/image";

interface AppImageProps {
  readonly url: string;
  readonly height?: number;
  readonly width?: number;
  readonly alt: string;
  readonly classes?: string;
}

/**
 * A custom responsive image component that adapts to the reverse proxy if set within this app.
 * 
 * @param {string} url - The url to the image. Either the relative url within the public directory or to a remote url. Do not add / at the start for relative url.
 * @param {number} height - Maximum rendered height in pixels.
 * @param {number} width - Maximum rendered width in pixels.
 * @param {string} alt - Alternate text to describe image for screen readers and search engines.
 * @returns A link element with the prefixed URL.
 */
export default function AppImage(props: AppImageProps) {
  const height: number = props.height ?? 25;
  const width: number = props.width ?? 25;
  // Despite optimisation benefits, do not use Next images. 
  // They employ a different url format in production, which cannot work well with the reverse proxy in the stack.
  return (
    <div className={props.classes}>
      <Image
        src={props.url}
        height={height}
        width={width}
        alt={props.alt}
      />
    </div>
  )
}