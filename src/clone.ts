/** Default folder name for `source`: its last path segment without a trailing ".git", as git clone picks it. */
export function cloneFolderName(source: string) {
  const segments = source.trim().split(/[\\/:]/).filter(Boolean);
  return (segments[segments.length - 1] ?? "").replace(/\.git$/i, "");
}

/** `name` inside `parent`, with the separator `parent` already uses; null when `name` is not a single folder name. */
export function cloneDestination(parent: string, name: string) {
  const folder = name.trim();
  if (!parent.trim() || !folder || folder === "." || folder === ".." || /[\\/]/.test(folder)) return null;
  const separator = parent.includes("\\") ? "\\" : "/";
  return `${parent.replace(/[\\/]+$/, "")}${separator}${folder}`;
}
