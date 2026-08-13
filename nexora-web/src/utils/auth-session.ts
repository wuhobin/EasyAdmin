export type UnauthorizedHandler = () => void | Promise<void>

let unauthorizedHandler: UnauthorizedHandler | null = null
let unauthorizedPromise: Promise<void> | null = null

export function registerUnauthorizedHandler(handler: UnauthorizedHandler) {
  unauthorizedHandler = handler
}

export function notifyUnauthorized() {
  if (!unauthorizedPromise) {
    unauthorizedPromise = Promise.resolve()
      .then(async () => unauthorizedHandler?.())
      .finally(() => {
        unauthorizedPromise = null
      })
  }
  return unauthorizedPromise
}
