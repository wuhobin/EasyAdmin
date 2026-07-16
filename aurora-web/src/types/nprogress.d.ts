declare module 'nprogress' {
  interface NProgressStatic {
    start(): void
    done(): void
    configure(options: {
      easing?: string
      minimum?: number
      showSpinner?: boolean
      speed?: number
      trickleSpeed?: number
    }): void
  }
  
  const nprogress: NProgressStatic
  export default nprogress
}
