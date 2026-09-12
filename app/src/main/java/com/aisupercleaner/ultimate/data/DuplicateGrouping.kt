package com.aisupercleaner.ultimate.data

object DuplicateGrouping {
    fun candidateGroups(files: List<FileMetadataEntity>): List<List<FileMetadataEntity>> =
        files
            .filter { it.sizeBytes > 0 }
            .groupBy { it.mediaType to it.sizeBytes }
            .values
            .filter { it.size > 1 }

    fun exactGroups(files: List<FileMetadataEntity>): List<DuplicateGroup> {
        val hashToFiles = linkedMapOf<String, MutableList<FileMetadataEntity>>()
        for (file in files) {
            val hash = file.contentHash ?: continue
            hashToFiles.getOrPut(hash) { mutableListOf() }.add(file)
        }
        return hashToFiles.values
            .filter { it.size > 1 }
            .map { group -> DuplicateGroup(group.first().contentHash.orEmpty(), group, group.drop(1).sumOf { it.sizeBytes }) }
    }
}
