package net.asianovel.reader.lib.permission

interface OnPermissionsResultCallback {

    fun onPermissionsGranted()

    fun onPermissionsDenied(deniedPermissions: Array<String>?)

    fun onError(e: Exception)

}