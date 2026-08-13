export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export class RequestError extends Error {
  constructor(
    message: string,
    readonly isUnauthorized = false,
    readonly isReported = false
  ) {
    super(message)
    this.name = 'RequestError'
  }
}

export function isUnauthorizedError(error: unknown): error is RequestError {
  return error instanceof RequestError && error.isUnauthorized
}

export function isReportedRequestError(error: unknown): error is RequestError {
  return error instanceof RequestError && error.isReported
}
