import defaultLogoUrl from '@/assets/brand/nexora-logo.svg'

interface BrandMarkProps {
  size?: number
  className?: string
  src?: string
}

export function BrandMark({ size = 32, className = '', src = defaultLogoUrl }: BrandMarkProps) {
  return (
    <span className={`brand-mark ${className}`} style={{ width: size, height: size }} aria-hidden="true">
      <img src={src} alt="" width={size} height={size} />
    </span>
  )
}
