import { useEffect, useRef, useState, type CSSProperties } from 'react'

interface CharacterPosition {
  faceX: number
  faceY: number
  bodySkew: number
}

interface LookProps {
  lookX: number
  lookY: number
  size?: number
  maxDistance?: number
  pupilColor?: string
  forceLookX?: number
  forceLookY?: number
}

const EMPTY_POSITION: CharacterPosition = { faceX: 0, faceY: 0, bodySkew: 0 }
const CHARACTER_CENTERS = {
  purple: { x: 160, y: 147 },
  black: { x: 300, y: 233 },
  orange: { x: 120, y: 307 },
  yellow: { x: 380, y: 287 }
} as const

function clampLook(value: number, maxDistance: number) {
  return Math.max(-maxDistance, Math.min(maxDistance, value))
}

function lookPosition({ lookX, lookY, maxDistance = 5, forceLookX, forceLookY }: LookProps) {
  return {
    x: forceLookX ?? clampLook(lookX / 3, maxDistance),
    y: forceLookY ?? clampLook(lookY / 2, maxDistance)
  }
}

function Pupil(props: LookProps) {
  const { size = 12, pupilColor = '#2D2D2D' } = props
  const position = lookPosition(props)
  return <div style={{ width: size, height: size, borderRadius: '50%', backgroundColor: pupilColor, transform: `translate3d(${position.x}px, ${position.y}px, 0)`, transition: 'transform 0.1s ease-out' }} />
}

interface EyeBallProps extends LookProps {
  pupilSize?: number
  eyeColor?: string
  isBlinking?: boolean
}

function EyeBall({ size = 18, pupilSize = 7, eyeColor = 'white', pupilColor = '#2D2D2D', isBlinking = false, ...props }: EyeBallProps) {
  const position = lookPosition(props)
  return (
    <div style={{ width: size, height: size, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', borderRadius: '50%', backgroundColor: eyeColor, transform: `scaleY(${isBlinking ? 0.12 : 1})`, transition: 'transform 0.15s ease', transformOrigin: 'center' }}>
      <div style={{ width: pupilSize, height: pupilSize, borderRadius: '50%', backgroundColor: pupilColor, transform: `translate3d(${position.x}px, ${position.y}px, 0)`, transition: 'transform 0.1s ease-out' }} />
    </div>
  )
}

function useBlinkLoop(setBlinking: (value: boolean) => void) {
  useEffect(() => {
    let active = true
    let timeout: number | undefined
    let blinkTimeout: number | undefined
    const run = () => {
      timeout = window.setTimeout(() => {
        if (!active) return
        setBlinking(true)
        blinkTimeout = window.setTimeout(() => {
          if (!active) return
          setBlinking(false)
          run()
        }, 150)
      }, Math.random() * 4000 + 3000)
    }
    run()
    return () => {
      active = false
      if (timeout) window.clearTimeout(timeout)
      if (blinkTimeout) window.clearTimeout(blinkTimeout)
    }
  }, [setBlinking])
}

function calculatePosition(mouseX: number, mouseY: number, center: { x: number; y: number }): CharacterPosition {
  const deltaX = mouseX - center.x
  const deltaY = mouseY - center.y
  return {
    faceX: Math.max(-15, Math.min(15, deltaX / 20)),
    faceY: Math.max(-10, Math.min(10, deltaY / 30)),
    bodySkew: Math.max(-6, Math.min(6, -deltaX / 120))
  }
}

export interface AuthAnimationState {
  isTyping?: boolean
  isPasswordFocused?: boolean
  showPassword?: boolean
  passwordLength?: number
  isSubmitting?: boolean
  hasError?: boolean
}

export function AnimatedCharacters({ isTyping = false, isPasswordFocused = false, showPassword = false, passwordLength = 0 }: AuthAnimationState) {
  const sceneRef = useRef<HTMLDivElement>(null)
  const [positions, setPositions] = useState({ purple: EMPTY_POSITION, black: EMPTY_POSITION, orange: EMPTY_POSITION, yellow: EMPTY_POSITION })
  const [isPurpleBlinking, setIsPurpleBlinking] = useState(false)
  const [isBlackBlinking, setIsBlackBlinking] = useState(false)
  const [isLookingAtEachOther, setIsLookingAtEachOther] = useState(false)
  const [isPurplePeeking, setIsPurplePeeking] = useState(false)

  useEffect(() => {
    let frame = 0
    let pointerX = 0
    let pointerY = 0
    const update = () => {
      frame = 0
      const scene = sceneRef.current
      if (!scene) return
      const rect = scene.getBoundingClientRect()
      const mouseX = pointerX - rect.left
      const mouseY = pointerY - rect.top
      setPositions({
        purple: calculatePosition(mouseX, mouseY, CHARACTER_CENTERS.purple),
        black: calculatePosition(mouseX, mouseY, CHARACTER_CENTERS.black),
        orange: calculatePosition(mouseX, mouseY, CHARACTER_CENTERS.orange),
        yellow: calculatePosition(mouseX, mouseY, CHARACTER_CENTERS.yellow)
      })
    }
    const handlePointerMove = (event: PointerEvent) => {
      pointerX = event.clientX
      pointerY = event.clientY
      if (!frame) frame = window.requestAnimationFrame(update)
    }
    window.addEventListener('pointermove', handlePointerMove, { passive: true })
    return () => {
      window.removeEventListener('pointermove', handlePointerMove)
      if (frame) window.cancelAnimationFrame(frame)
    }
  }, [])

  useBlinkLoop(setIsPurpleBlinking)
  useBlinkLoop(setIsBlackBlinking)

  useEffect(() => {
    if (!isTyping) {
      setIsLookingAtEachOther(false)
      return
    }
    setIsLookingAtEachOther(true)
    const timer = window.setTimeout(() => setIsLookingAtEachOther(false), 800)
    return () => window.clearTimeout(timer)
  }, [isTyping])

  useEffect(() => {
    if (passwordLength === 0 || !showPassword) {
      setIsPurplePeeking(false)
      return
    }
    let active = true
    let closeTimer: number | undefined
    const peekTimer = window.setTimeout(() => {
      if (!active) return
      setIsPurplePeeking(true)
      closeTimer = window.setTimeout(() => {
        if (active) setIsPurplePeeking(false)
      }, 800)
    }, Math.random() * 3000 + 2000)
    return () => {
      active = false
      window.clearTimeout(peekTimer)
      if (closeTimer) window.clearTimeout(closeTimer)
    }
  }, [passwordLength, showPassword])

  const isHidingPassword = passwordLength > 0 && !showPassword
  const isLookingAway = isPasswordFocused && !showPassword
  const transition = isPasswordFocused || isTyping ? 'transform 0.6s ease-out' : 'transform 0.1s ease-out'
  const characterStyle = (backgroundColor: string, zIndex: number, dimensions: CSSProperties, transform: string): CSSProperties => ({
    position: 'absolute',
    bottom: '-2px',
    zIndex,
    transform,
    transformOrigin: 'bottom center',
    backfaceVisibility: 'hidden',
    transition,
    borderBottom: `4px solid ${backgroundColor}`,
    backgroundColor,
    ...dimensions
  })
  const faceStyle = (left: number, top: number, gap: number, position: CharacterPosition, forced?: { x: number; y: number }): CSSProperties => ({
    position: 'absolute',
    left,
    top,
    display: 'flex',
    gap,
    transform: `translate3d(${forced?.x ?? position.faceX}px, ${forced?.y ?? position.faceY}px, 0)`,
    transition: 'transform 0.2s ease-out'
  })

  const purpleForced = isLookingAway ? { x: -25, y: -15 } : passwordLength > 0 && showPassword ? { x: -25, y: -5 } : isLookingAtEachOther ? { x: 10, y: 25 } : undefined
  const blackForced = isLookingAway ? { x: -16, y: -12 } : passwordLength > 0 && showPassword ? { x: -16, y: -4 } : isLookingAtEachOther ? { x: 6, y: -20 } : undefined
  const orangeForced = isLookingAway ? { x: -32, y: -15 } : passwordLength > 0 && showPassword ? { x: -32, y: -5 } : undefined
  const yellowForced = isLookingAway ? { x: -32, y: -10 } : passwordLength > 0 && showPassword ? { x: -32, y: -5 } : undefined

  return (
    <div ref={sceneRef} className="characters-scene" aria-hidden="true">
      <div style={characterStyle('#6C3FF5', 1, { left: 70, width: 180, height: 440, borderRadius: '10px 10px 0 0' }, `${passwordLength > 0 && showPassword ? 'skewX(0deg)' : isLookingAway ? 'skewX(-14deg) translateX(-20px)' : isTyping || isHidingPassword ? `skewX(${positions.purple.bodySkew - 12}deg) translateX(40px)` : `skewX(${positions.purple.bodySkew}deg)`} scaleY(${isLookingAway || isTyping || isHidingPassword ? 1 : 0.91}) translateZ(0)`)}>
        <div style={faceStyle(45, 40, 32, positions.purple, purpleForced)}>
          <EyeBall lookX={positions.purple.faceX} lookY={positions.purple.faceY} isBlinking={isPurpleBlinking} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 4 : -4 : isLookingAtEachOther ? 3 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 5 : -4 : isLookingAtEachOther ? 4 : undefined} />
          <EyeBall lookX={positions.purple.faceX} lookY={positions.purple.faceY} isBlinking={isPurpleBlinking} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 4 : -4 : isLookingAtEachOther ? 3 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 5 : -4 : isLookingAtEachOther ? 4 : undefined} />
        </div>
      </div>

      <div style={characterStyle('#2D2D2D', 2, { left: 240, width: 120, height: 310, borderRadius: '8px 8px 0 0' }, `${passwordLength > 0 && showPassword ? 'skewX(0deg)' : isLookingAway ? 'skewX(12deg) translateX(-10px)' : isLookingAtEachOther ? `skewX(${positions.black.bodySkew * 1.5 + 10}deg) translateX(20px)` : `skewX(${positions.black.bodySkew * 1.5}deg)`} translateZ(0)`)}>
        <div style={faceStyle(26, 32, 24, positions.black, blackForced)}>
          <EyeBall lookX={positions.black.faceX} lookY={positions.black.faceY} size={16} pupilSize={6} isBlinking={isBlackBlinking} forceLookX={isLookingAway ? -4 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? 0 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? -4 : undefined} />
          <EyeBall lookX={positions.black.faceX} lookY={positions.black.faceY} size={16} pupilSize={6} isBlinking={isBlackBlinking} forceLookX={isLookingAway ? -4 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? 0 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? -4 : undefined} />
        </div>
      </div>

      <div style={characterStyle('#FF9B6B', 3, { left: 0, width: 240, height: 200, borderRadius: '120px 120px 0 0' }, `${passwordLength > 0 && showPassword ? 'skewX(0deg)' : `skewX(${positions.orange.bodySkew}deg)`} translateZ(0)`)}>
        <div style={faceStyle(82, 90, 32, positions.orange, orangeForced)}>
          <Pupil lookX={positions.orange.faceX} lookY={positions.orange.faceY} forceLookX={isLookingAway || passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
          <Pupil lookX={positions.orange.faceX} lookY={positions.orange.faceY} forceLookX={isLookingAway || passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
        </div>
      </div>

      <div style={characterStyle('#E8D754', 4, { left: 310, width: 140, height: 230, borderRadius: '70px 70px 0 0' }, `${passwordLength > 0 && showPassword ? 'skewX(0deg)' : `skewX(${positions.yellow.bodySkew}deg)`} translateZ(0)`)}>
        <div style={faceStyle(52, 40, 24, positions.yellow, yellowForced)}>
          <Pupil lookX={positions.yellow.faceX} lookY={positions.yellow.faceY} forceLookX={isLookingAway || passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
          <Pupil lookX={positions.yellow.faceX} lookY={positions.yellow.faceY} forceLookX={isLookingAway || passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
        </div>
        <span style={{ position: 'absolute', left: 40, top: 88, width: 80, height: 4, borderRadius: 999, backgroundColor: '#2D2D2D', transform: `translate3d(${yellowForced?.x ?? positions.yellow.faceX}px, ${yellowForced?.y ?? positions.yellow.faceY}px, 0)`, transition: 'transform 0.2s ease-out' }} />
      </div>
    </div>
  )
}
