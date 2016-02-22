package de.blau.android.dialogs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.SherlockDialogFragment;

import de.blau.android.ErrorCodes;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ThemeUtils;

/**
 * Simple alert dialog with an OK button that does nothing
 * @author simon
 *
 */
public class ErrorAlertDialogFragment extends SherlockDialogFragment
{
	
	private static final String DEBUG_TAG = ErrorAlertDialogFragment.class.getSimpleName();
		
	private int titleId;
	private int messageId;
	
	static public void showDialog(FragmentActivity activity, int errorCode) {
		dismissDialog(activity, errorCode);

		FragmentManager fm = activity.getSupportFragmentManager();
	    ErrorAlertDialogFragment alertDialogFragment = newInstance(errorCode);
	    if (alertDialogFragment != null) {
	    	alertDialogFragment.show(fm, getTag(errorCode));
	    } else {
	    	Log.e(DEBUG_TAG,"Unable to create dialog for value " + errorCode);
	    }
	}
	
	static public void dismissDialog(FragmentActivity activity, int errorCode) {
		FragmentManager fm = activity.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
	    Fragment fragment = fm.findFragmentByTag(getTag(errorCode));
	    if (fragment != null) {
	        ft.remove(fragment);
	    }
	    ft.commit();
	}
	
	private static String getTag(int errorCode) {
		switch (errorCode) {
		case ErrorCodes.NO_LOGIN_DATA:
			return "alert_no_login_data";
		case ErrorCodes.NO_CONNECTION:
			return "alert_no_connection";
		case ErrorCodes.UPLOAD_PROBLEM:
			return "alert_upload_problem";
		case ErrorCodes.DATA_CONFLICT:
			return "alert_data_conflict";
		case ErrorCodes.API_OFFLINE:
			return "alert_api_offline";
		case ErrorCodes.OUT_OF_MEMORY:
			return "alert_out_of_memory";
		case ErrorCodes.OUT_OF_MEMORY_DIRTY:
			return "alert_out_of_memory_dirty";
		case ErrorCodes.INVALID_DATA_RECEIVED:
			return "alert_invalid_data_received";
		case ErrorCodes.FILE_WRITE_FAILED:
			return "alert_file_write_failed";
		}
		return null;
	}
	
	static private ErrorAlertDialogFragment newInstance(int dialogType) {
		switch (dialogType) {
		case ErrorCodes.NO_LOGIN_DATA: return createNewInstance(R.string.no_login_data_title, R.string.no_login_data_message);		
		case ErrorCodes.NO_CONNECTION: return createNewInstance(R.string.no_connection_title, R.string.no_connection_message);
		case ErrorCodes.UPLOAD_PROBLEM: return createNewInstance(R.string.upload_problem_title, R.string.upload_problem_message);
		case ErrorCodes.DATA_CONFLICT: return createNewInstance(R.string.data_conflict_title, R.string.data_conflict_message);
		case ErrorCodes.API_OFFLINE: return createNewInstance(R.string.api_offline_title, R.string.api_offline_message);
		case ErrorCodes.OUT_OF_MEMORY: return createNewInstance(R.string.out_of_memory_title, R.string.out_of_memory_message);
		case ErrorCodes.OUT_OF_MEMORY_DIRTY: return createNewInstance(R.string.out_of_memory_title, R.string.out_of_memory_dirty_message);
		case ErrorCodes.INVALID_DATA_RECEIVED: return createNewInstance(R.string.invalid_data_received_title, R.string.invalid_data_received_message);
		case ErrorCodes.FILE_WRITE_FAILED: return createNewInstance( R.string.file_write_failed_title, R.string.file_write_failed_message);
		}	
		return null;
	}
		
    /**
     */
    static private ErrorAlertDialogFragment createNewInstance(final int titleId, final int messageId) {
    	ErrorAlertDialogFragment f = new ErrorAlertDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable("title", titleId);
        args.putSerializable("message", messageId);

        f.setArguments(args);
        f.setShowsDialog(true);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        titleId = (Integer) getArguments().getSerializable("title");
        messageId = (Integer) getArguments().getSerializable("message");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
    	Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(),R.attr.alert_dialog));
		builder.setTitle(titleId);
		if (messageId != 0) {
			builder.setMessage(messageId);
		}
		DoNothingListener doNothingListener = new DoNothingListener();
		builder.setPositiveButton(R.string.okay, doNothingListener);
		return builder.create();
    }	
}
