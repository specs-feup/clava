export default interface AbstractDumper {
  dump(): Promise<Record<string, unknown>>;
}
