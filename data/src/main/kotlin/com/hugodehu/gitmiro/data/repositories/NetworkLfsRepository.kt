package com.hugodehu.gitmiro.data.repositories

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.lfs.LfsObjectBatch
import com.hugodehu.gitmiro.domain.lfs.LfsObjects
import com.hugodehu.gitmiro.domain.lfs.LfsPrepareUploadObjectBatch
import com.hugodehu.gitmiro.domain.lfs.LfsRef
import com.hugodehu.gitmiro.domain.models.OperationType
import com.hugodehu.gitmiro.domain.repositories.LfsRepository
import javax.inject.Inject

private const val TAG = "LfsRepository"

class NetworkLfsRepository @Inject constructor(
    private val lfsNetworkDataSource: LfsNetworkDataSource,
) : LfsRepository by lfsNetworkDataSource {
    override suspend fun getLfsObjects(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        objects: List<LfsObjectBatch>,
        username: String?,
        password: String?,
        headers: Map<String, String>,
    ): Either<LfsObjects, LfsError> {
        return postBatchObjects(
            lfsServerUrl,
            createLfsPrepareUploadObjectBatch(
                operationType,
                branch = branch,
                objects = objects,
            ),
            headers = headers,
            username,
            password,
        )
    }

    private fun createLfsPrepareUploadObjectBatch(
        operation: OperationType,
        algo: String = "sha256",
        branch: String,
        objects: List<LfsObjectBatch>,
    ): LfsPrepareUploadObjectBatch {
        return LfsPrepareUploadObjectBatch(
            operation = operation.value,
            objects = objects,
            transfers = listOf(
                "basic",
                // TODO Add support for standalone files and SSH once they are stable https://github.com/git-lfs/git-lfs/blob/main/docs/api/README.md
                "lfs-standalone-file",
                "ssh",
            ),
            ref = LfsRef(branch),
            hashAlgo = algo,
        )
    }
}


