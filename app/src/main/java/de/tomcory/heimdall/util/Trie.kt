package de.tomcory.heimdall.util

class Trie<T>(private val pathSplitter: (String) -> List<String>) {
    private val root = TrieNode<T>()

    fun insert(path: String, item: T) {
        var node = root
        val parts = pathSplitter(path)
        for (part in parts) {
            if (part !in node.children) {
                node.children[part] = TrieNode()
            }
            node = node.children[part]!!
        }
        node.isTerminating = true
        node.item = item
    }

    fun search(path: String): T? {
        var node = root
        val parts = pathSplitter(path)
        for (part in parts) {
            if (part !in node.children) {
                return null
            }
            node = node.children[part]!!
            if (node.isTerminating) {
                return node.item
            }
        }
        return null
    }
}

private class TrieNode<T>(var isTerminating: Boolean = false) {
    val children: MutableMap<String, TrieNode<T>> = mutableMapOf()
    var item: T? = null
}