package com.robin.camerax.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.robin.camerax.R
import com.robin.camerax.component.ContextViewModelFactory
import com.robin.camerax.viewmodel.BaseViewModel
import com.robin.libutil.LogUtil
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<DB : ViewDataBinding, VM : BaseViewModel> : Fragment() {
    protected lateinit var binding: DB
    protected lateinit var viewModel: VM
    private lateinit var permissonWarnDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = binding(inflater, container, savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        //init view model
        val genericSuperclass = javaClass.genericSuperclass
        if (genericSuperclass != null) {
            val parameterizedType = genericSuperclass as ParameterizedType
            val actualTypeArguments = parameterizedType.actualTypeArguments
            val homeViewModelClass = actualTypeArguments[1] as Class<ViewModel>
            viewModel = ViewModelProvider(context as AppCompatActivity, ContextViewModelFactory(this.requireContext())).get(homeViewModelClass) as VM
        }
        onViewCreatedInner()
        if (!hasPermissions(requireContext())) {
            requestPermissions(permissons()!!, PERMISSIONS_REQUEST_CODE_PERMISSIONS)
        } else {
            afterHasPermisson()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE_PERMISSIONS) {
            for (i in grantResults.indices) {
                val item = grantResults[i]
                if (item != PackageManager.PERMISSION_GRANTED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            permissions[i]
                        )
                    ) {
                        LogUtil.d("shouldShowRequestPermissionRationale")
                        if (permissonWarnDialog == null) {
                            val builder = AlertDialog.Builder(requireContext())
                            builder.setMessage(context?.getString(R.string.dialog_permisson_msg))
                            builder.setPositiveButton(
                                    context?.getString(R.string.dialog_permisson_pos)
                            ) { _: DialogInterface?, _: Int ->
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts(
                                    "package",
                                        context?.packageName,
                                    null
                                )
                                intent.data = uri
                                context?.startActivity(intent)
                            }
                            builder.setNegativeButton(
                                    context?.getString(R.string.dialog_permisson_neg)
                            ) { _: DialogInterface?, _: Int -> activity!!.finish() }
                            permissonWarnDialog = builder.create()
                        }
                        if (!permissonWarnDialog.isShowing) {
                            permissonWarnDialog.show()
                        }
                    } else {
                        requestPermissions(permissons()!!, PERMISSIONS_REQUEST_CODE_PERMISSIONS)
                    }
                    return
                }
            }
            LogUtil.d("VIDEO PERMISSION_GRANTED")
            afterHasPermisson()
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        val permissons = permissons()
        if (permissons == null || permissons.isEmpty()) {
        } else {
            for (item in permissons) {
                if (ContextCompat.checkSelfPermission(context.applicationContext, item) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    protected abstract fun binding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): DB

    protected abstract fun onViewCreatedInner()
    protected abstract fun permissons(): Array<String>?
    protected abstract fun afterHasPermisson()

    companion object {
        var PERMISSIONS_REQUEST_CODE_PERMISSIONS = 0x01
    }
}