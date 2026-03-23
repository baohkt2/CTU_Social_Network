import { describe, it, expect } from "vitest";
import { PluginRegistry } from "../plugins/registry.js";
import type { AnalyzerPlugin, StructuralAnalysis, ImportResolution } from "../types.js";

const emptyAnalysis: StructuralAnalysis = {
  functions: [],
  classes: [],
  imports: [],
  exports: [],
};

function createMockPlugin(name: string, languages: string[]): AnalyzerPlugin {
  return {
    name,
    languages,
    analyzeFile: () => ({ ...emptyAnalysis }),
    resolveImports: () => [],
  };
}

describe("PluginRegistry", () => {
  it("registers a plugin", () => {
    const registry = new PluginRegistry();
    const plugin = createMockPlugin("test", ["typescript"]);
    registry.register(plugin);
    expect(registry.getPlugins()).toHaveLength(1);
  });

  it("finds plugin by language", () => {
    const registry = new PluginRegistry();
    const plugin = createMockPlugin("ts-plugin", ["typescript", "javascript"]);
    registry.register(plugin);
    expect(registry.getPluginForLanguage("typescript")).toBe(plugin);
    expect(registry.getPluginForLanguage("javascript")).toBe(plugin);
  });

  it("returns null for unsupported language", () => {
    const registry = new PluginRegistry();
    registry.register(createMockPlugin("ts-plugin", ["typescript"]));
    expect(registry.getPluginForLanguage("python")).toBeNull();
  });

  it("finds plugin by file extension", () => {
    const registry = new PluginRegistry();
    const plugin = createMockPlugin("ts-plugin", ["typescript"]);
    registry.register(plugin);
    expect(registry.getPluginForFile("src/index.ts")).toBe(plugin);
    expect(registry.getPluginForFile("src/app.tsx")).toBe(plugin);
  });

  it("maps common extensions to languages", () => {
    const registry = new PluginRegistry();
    const plugin = createMockPlugin("multi", ["python", "go", "rust"]);
    registry.register(plugin);
    expect(registry.getPluginForFile("main.py")).toBe(plugin);
    expect(registry.getPluginForFile("main.go")).toBe(plugin);
    expect(registry.getPluginForFile("main.rs")).toBe(plugin);
  });

  it("lists all registered plugins", () => {
    const registry = new PluginRegistry();
    registry.register(createMockPlugin("a", ["typescript"]));
    registry.register(createMockPlugin("b", ["python"]));
    expect(registry.getPlugins()).toHaveLength(2);
  });

  it("lists supported languages", () => {
    const registry = new PluginRegistry();
    registry.register(createMockPlugin("a", ["typescript", "javascript"]));
    registry.register(createMockPlugin("b", ["python"]));
    const langs = registry.getSupportedLanguages();
    expect(langs).toContain("typescript");
    expect(langs).toContain("python");
  });

  it("unregisters a plugin by name", () => {
    const registry = new PluginRegistry();
    registry.register(createMockPlugin("removable", ["typescript"]));
    expect(registry.getPlugins()).toHaveLength(1);
    registry.unregister("removable");
    expect(registry.getPlugins()).toHaveLength(0);
  });

  it("later registration takes priority for same language", () => {
    const registry = new PluginRegistry();
    const first = createMockPlugin("first", ["typescript"]);
    const second = createMockPlugin("second", ["typescript"]);
    registry.register(first);
    registry.register(second);
    expect(registry.getPluginForLanguage("typescript")?.name).toBe("second");
  });

  it("analyzeFile delegates to correct plugin", () => {
    const registry = new PluginRegistry();
    const plugin = createMockPlugin("ts-plugin", ["typescript"]);
    plugin.analyzeFile = () => ({
      ...emptyAnalysis,
      functions: [{ name: "hello", lineRange: [1, 5], params: [] }],
    });
    registry.register(plugin);

    const result = registry.analyzeFile("src/test.ts", "const x = 1;");
    expect(result).not.toBeNull();
    expect(result!.functions).toHaveLength(1);
  });

  it("analyzeFile returns null for unsupported files", () => {
    const registry = new PluginRegistry();
    registry.register(createMockPlugin("ts-plugin", ["typescript"]));
    const result = registry.analyzeFile("main.py", "print('hello')");
    expect(result).toBeNull();
  });
});
