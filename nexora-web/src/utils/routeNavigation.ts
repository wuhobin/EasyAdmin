export function isExternalPath(path: string) {
  return /^(https?:|mailto:|tel:)/i.test(path)
}
