import { useEffect, useRef, useState, type CSSProperties, type RefObject } from 'react'

interface CharacterPosition {
  faceX: number
  faceY: number
  bodySkew: number
}

interface PupilProps {
  mouseX: number
  mouseY: number
  size?: number
  maxDistance?: number
  pupilColor?: string
  forceLookX?: number
  forceLookY?: number
}

function Pupil({ mouseX, mouseY, size = 12, maxDistance = 5, pupilColor = '#2D2D2D', forceLookX, forceLookY }: PupilProps) {
  const pupilRef = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState({ x: 0, y: 0 })

  useEffect(() => {
    if (!pupilRef.current) return
    if (forceLookX !== undefined && forceLookY !== undefined) {
      setPosition({ x: forceLookX, y: forceLookY })
      return
    }

    const pupil = pupilRef.current.getBoundingClientRect()
    const deltaX = mouseX - (pupil.left + pupil.width / 2)
    const deltaY = mouseY - (pupil.top + pupil.height / 2)
    const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), maxDistance)
    const angle = Math.atan2(deltaY, deltaX)
    setPosition({ x: Math.cos(angle) * distance, y: Math.sin(angle) * distance })
  }, [mouseX, mouseY, forceLookX, forceLookY, maxDistance])

  return (
    <div
      ref={pupilRef}
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        backgroundColor: pupilColor,
        transform: `translate(${position.x}px, ${position.y}px)`,
        transition: 'transform 0.1s ease-out'
      }}
    />
  )
}

interface EyeBallProps extends PupilProps {
  size?: number
  pupilSize?: number
  eyeColor?: string
  isBlinking?: boolean
}

function EyeBall({ mouseX, mouseY, size = 18, pupilSize = 7, maxDistance = 5, eyeColor = 'white', pupilColor = '#2D2D2D', isBlinking = false, forceLookX, forceLookY }: EyeBallProps) {
  const eyeRef = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState({ x: 0, y: 0 })

  useEffect(() => {
    if (!eyeRef.current) return
    if (forceLookX !== undefined && forceLookY !== undefined) {
      setPosition({ x: forceLookX, y: forceLookY })
      return
    }

    const eye = eyeRef.current.getBoundingClientRect()
    const deltaX = mouseX - (eye.left + eye.width / 2)
    const deltaY = mouseY - (eye.top + eye.height / 2)
    const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), maxDistance)
    const angle = Math.atan2(deltaY, deltaX)
    setPosition({ x: Math.cos(angle) * distance, y: Math.sin(angle) * distance })
  }, [mouseX, mouseY, forceLookX, forceLookY, maxDistance])

  return (
    <div
      ref={eyeRef}
      style={{
        width: size,
        height: isBlinking ? 2 : size,
        borderRadius: '50%',
        backgroundColor: eyeColor,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        transition: 'height 0.15s ease'
      }}
    >
      {!isBlinking ? <div style={{ width: pupilSize, height: pupilSize, borderRadius: '50%', backgroundColor: pupilColor, transform: `translate(${position.x}px, ${position.y}px)`, transition: 'transform 0.1s ease-out' }} /> : null}
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

export interface AuthAnimationState {
  isTyping?: boolean
  isPasswordFocused?: boolean
  showPassword?: boolean
  passwordLength?: number
  isSubmitting?: boolean
  hasError?: boolean
}

export function AnimatedCharacters({ isTyping = false, isPasswordFocused = false, showPassword = false, passwordLength = 0 }: AuthAnimationState) {
  const [mouseX, setMouseX] = useState(0)
  const [mouseY, setMouseY] = useState(0)
  const [isPurpleBlinking, setIsPurpleBlinking] = useState(false)
  const [isBlackBlinking, setIsBlackBlinking] = useState(false)
  const [isLookingAtEachOther, setIsLookingAtEachOther] = useState(false)
  const [isPurplePeeking, setIsPurplePeeking] = useState(false)

  const purpleRef = useRef<HTMLDivElement>(null)
  const blackRef = useRef<HTMLDivElement>(null)
  const yellowRef = useRef<HTMLDivElement>(null)
  const orangeRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleMouseMove = (event: MouseEvent) => {
      setMouseX(event.clientX)
      setMouseY(event.clientY)
    }
    window.addEventListener('mousemove', handleMouseMove)
    return () => window.removeEventListener('mousemove', handleMouseMove)
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
    const peek = window.setTimeout(() => {
      if (!active) return
      setIsPurplePeeking(true)
      window.setTimeout(() => {
        if (active) setIsPurplePeeking(false)
      }, 800)
    }, Math.random() * 3000 + 2000)
    return () => {
      active = false
      window.clearTimeout(peek)
    }
  }, [passwordLength, showPassword])

  const calculatePosition = (ref: RefObject<HTMLDivElement>): CharacterPosition => {
    if (!ref.current) return { faceX: 0, faceY: 0, bodySkew: 0 }
    const rect = ref.current.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 3
    const deltaX = mouseX - centerX
    const deltaY = mouseY - centerY
    return {
      faceX: Math.max(-15, Math.min(15, deltaX / 20)),
      faceY: Math.max(-10, Math.min(10, deltaY / 30)),
      bodySkew: Math.max(-6, Math.min(6, -deltaX / 120))
    }
  }

  const purplePos = calculatePosition(purpleRef)
  const blackPos = calculatePosition(blackRef)
  const yellowPos = calculatePosition(yellowRef)
  const orangePos = calculatePosition(orangeRef)
  const isHidingPassword = passwordLength > 0 && !showPassword
  const isLookingAway = isPasswordFocused && !showPassword

  const getCharStyle = (backgroundColor: string, zIndex: number, position: CharacterPosition, dimensions: CSSProperties, transform?: string): CSSProperties => {
    const transformTransition = isPasswordFocused || isTyping ? 'transform 0.6s ease-out' : 'transform 0.1s ease-out'
    return {
      position: 'absolute',
      backgroundColor,
      zIndex,
      transformOrigin: 'bottom center',
      willChange: 'transform',
      backfaceVisibility: 'hidden',
      WebkitBackfaceVisibility: 'hidden',
      transition: `${transformTransition}, height 0.6s ease-in-out`,
      transform: transform || `skewX(${position.bodySkew}deg) translateZ(0)`,
      bottom: '-2px',
      borderBottom: `4px solid ${backgroundColor}`,
      ...dimensions
    }
  }

  return (
    <div className="characters-scene" aria-hidden="true">
      <div
        ref={purpleRef}
        style={getCharStyle('#6C3FF5', 1, purplePos, { left: 70, width: 180, height: isLookingAway || isTyping || isHidingPassword ? 440 : 400, borderRadius: '10px 10px 0 0' }, passwordLength > 0 && showPassword ? 'skewX(0deg) translateZ(0)' : isLookingAway ? 'skewX(-14deg) translateX(-20px) translateZ(0)' : isTyping || isHidingPassword ? `skewX(${purplePos.bodySkew - 12}deg) translateX(40px) translateZ(0)` : undefined)}
      >
        <div style={{ position: 'absolute', display: 'flex', gap: 32, left: isLookingAway ? 20 : passwordLength > 0 && showPassword ? 20 : isLookingAtEachOther ? 55 : 45 + purplePos.faceX, top: isLookingAway ? 25 : passwordLength > 0 && showPassword ? 35 : isLookingAtEachOther ? 65 : 40 + purplePos.faceY, transition: 'all 0.6s ease-out' }}>
          <EyeBall mouseX={mouseX} mouseY={mouseY} isBlinking={isPurpleBlinking} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 4 : -4 : isLookingAtEachOther ? 3 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 5 : -4 : isLookingAtEachOther ? 4 : undefined} />
          <EyeBall mouseX={mouseX} mouseY={mouseY} isBlinking={isPurpleBlinking} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 4 : -4 : isLookingAtEachOther ? 3 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? isPurplePeeking ? 5 : -4 : isLookingAtEachOther ? 4 : undefined} />
        </div>
      </div>

      <div
        ref={blackRef}
        style={getCharStyle('#2D2D2D', 2, blackPos, { left: 240, width: 120, height: 310, borderRadius: '8px 8px 0 0' }, passwordLength > 0 && showPassword ? 'skewX(0deg) translateZ(0)' : isLookingAway ? 'skewX(12deg) translateX(-10px) translateZ(0)' : isLookingAtEachOther ? `skewX(${blackPos.bodySkew * 1.5 + 10}deg) translateX(20px) translateZ(0)` : `skewX(${blackPos.bodySkew * 1.5}deg) translateZ(0)`)}
      >
        <div style={{ position: 'absolute', display: 'flex', gap: 24, left: isLookingAway ? 10 : passwordLength > 0 && showPassword ? 10 : isLookingAtEachOther ? 32 : 26 + blackPos.faceX, top: isLookingAway ? 20 : passwordLength > 0 && showPassword ? 28 : isLookingAtEachOther ? 12 : 32 + blackPos.faceY, transition: 'all 0.6s ease-out' }}>
          <EyeBall mouseX={mouseX} mouseY={mouseY} size={16} pupilSize={6} isBlinking={isBlackBlinking} forceLookX={isLookingAway ? -4 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? 0 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? -4 : undefined} />
          <EyeBall mouseX={mouseX} mouseY={mouseY} size={16} pupilSize={6} isBlinking={isBlackBlinking} forceLookX={isLookingAway ? -4 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? 0 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : isLookingAtEachOther ? -4 : undefined} />
        </div>
      </div>

      <div ref={orangeRef} style={getCharStyle('#FF9B6B', 3, orangePos, { left: 0, width: 240, height: 200, borderRadius: '120px 120px 0 0' }, passwordLength > 0 && showPassword ? 'skewX(0deg) translateZ(0)' : undefined)}>
        <div style={{ position: 'absolute', display: 'flex', gap: 32, left: isLookingAway ? 50 : passwordLength > 0 && showPassword ? 50 : 82 + orangePos.faceX, top: isLookingAway ? 75 : passwordLength > 0 && showPassword ? 85 : 90 + orangePos.faceY, transition: 'all 0.2s ease-out' }}>
          <Pupil mouseX={mouseX} mouseY={mouseY} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
          <Pupil mouseX={mouseX} mouseY={mouseY} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
        </div>
      </div>

      <div ref={yellowRef} style={getCharStyle('#E8D754', 4, yellowPos, { left: 310, width: 140, height: 230, borderRadius: '70px 70px 0 0' }, passwordLength > 0 && showPassword ? 'skewX(0deg) translateZ(0)' : undefined)}>
        <div style={{ position: 'absolute', display: 'flex', gap: 24, left: isLookingAway ? 20 : passwordLength > 0 && showPassword ? 20 : 52 + yellowPos.faceX, top: isLookingAway ? 30 : passwordLength > 0 && showPassword ? 35 : 40 + yellowPos.faceY, transition: 'all 0.2s ease-out' }}>
          <Pupil mouseX={mouseX} mouseY={mouseY} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
          <Pupil mouseX={mouseX} mouseY={mouseY} forceLookX={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -5 : undefined} forceLookY={isLookingAway ? -5 : passwordLength > 0 && showPassword ? -4 : undefined} />
        </div>
        <span style={{ position: 'absolute', width: 80, height: 4, left: isLookingAway ? 15 : passwordLength > 0 && showPassword ? 10 : 40 + yellowPos.faceX, top: isLookingAway ? 78 : passwordLength > 0 && showPassword ? 88 : 88 + yellowPos.faceY, borderRadius: 999, backgroundColor: '#2D2D2D', transition: 'all 0.2s ease-out' }} />
      </div>

    </div>
  )
}
