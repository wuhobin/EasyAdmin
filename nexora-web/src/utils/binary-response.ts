export interface BinaryApiError {
  code: number
  message: string
  data?: unknown
}

export async function readBlobApiError(data: unknown): Promise<BinaryApiError | null> {
  if (!(data instanceof Blob)) return null

  const mediaType = data.type.split(';', 1)[0].trim().toLowerCase()
  if (mediaType !== 'application/json' && !mediaType.endsWith('+json')) return null

  try {
    const parsed = JSON.parse(await data.text()) as Partial<BinaryApiError>
    if (typeof parsed.code !== 'number' || typeof parsed.message !== 'string') return null
    return parsed as BinaryApiError
  } catch {
    return null
  }
}
