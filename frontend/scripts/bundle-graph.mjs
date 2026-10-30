/** Only eager code imports belong to a route's first load; dynamic routes stay lazy. */
export function initialCodeFiles(outputs, roots) {
    const visited = new Set();
    const visit = path => {
        if (visited.has(path))
            return;
        const output = outputs[path];
        if (!output)
            throw new Error('Build output missing from stats: ' + path);
        if (!/\.(js|css)$/.test(path))
            return;
        visited.add(path);
        for (const dependency of output.imports || []) {
            if (dependency.external || dependency.kind === 'dynamic-import')
                continue;
            if (outputs[dependency.path])
                visit(dependency.path);
            else if (dependency.path.endsWith('.js'))
                throw new Error('Eager JavaScript dependency missing from stats: ' + dependency.path);
        }
    };
    roots.forEach(visit);
    return [...visited].sort();
}
export function entryOutput(outputs, entryPoint) {
    const result = Object.entries(outputs).find(([, output]) => output.entryPoint === entryPoint)?.[0];
    if (!result)
        throw new Error('Entry point missing from build stats: ' + entryPoint);
    return result;
}
