package com.example.bossfinance.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.bossfinance.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Utility class for providing consistent user feedback throughout the app
 */
class FeedbackUtils {
    
    companion object {
        private const val TOAST_SHORT_DURATION = Toast.LENGTH_SHORT
        private const val TOAST_LONG_DURATION = Toast.LENGTH_LONG
        
        // Show a short toast message
        fun showToast(context: Context, message: String) {
            Toast.makeText(context, message, TOAST_SHORT_DURATION).show()
        }
        
        // Show a short toast message from resource ID
        fun showToast(context: Context, @StringRes messageResId: Int) {
            Toast.makeText(context, messageResId, TOAST_SHORT_DURATION).show()
        }
        
        // Show a long toast message
        fun showLongToast(context: Context, message: String) {
            Toast.makeText(context, message, TOAST_LONG_DURATION).show()
        }
        
        // Show a long toast message from resource ID
        fun showLongToast(context: Context, @StringRes messageResId: Int) {
            Toast.makeText(context, messageResId, TOAST_LONG_DURATION).show()
        }
        
        // Show a success snackbar
        fun showSuccessSnackbar(view: View, message: String) {
            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            snackbarView.setBackgroundColor(ContextCompat.getColor(view.context, R.color.success_green))
            snackbar.show()
        }
        
        // Show an error snackbar
        fun showErrorSnackbar(view: View, message: String) {
            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            snackbarView.setBackgroundColor(ContextCompat.getColor(view.context, R.color.error_red))
            snackbar.show()
        }
        
        // Show an informational snackbar with action
        fun showActionSnackbar(
            view: View, 
            message: String, 
            actionText: String, 
            action: () -> Unit
        ) {
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                .setAction(actionText) { action.invoke() }
                .show()
        }
        
        // Show a confirmation dialog
        fun showConfirmationDialog(
            context: Context,
            title: String,
            message: String,
            positiveButtonText: String,
            negativeButtonText: String,
            onPositiveClick: () -> Unit,
            onNegativeClick: () -> Unit = {}
        ) {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClick.invoke() }
                .setNegativeButton(negativeButtonText) { _, _ -> onNegativeClick.invoke() }
                .show()
        }
        
        // Show a simple alert dialog
        fun showAlertDialog(
            context: Context,
            title: String,
            message: String,
            buttonText: String = context.getString(android.R.string.ok)
        ) {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, null)
                .show()
        }
        
        // Show an error dialog with icon
        fun showErrorDialog(
            context: Context,
            title: String,
            message: String,
            buttonText: String = context.getString(android.R.string.ok)
        ) {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.ic_error)
                .setPositiveButton(buttonText, null)
                .show()
        }
    }
}