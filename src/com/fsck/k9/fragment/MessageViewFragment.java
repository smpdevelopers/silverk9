package com.fsck.k9.fragment;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.provider.MessagerProvider;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.SingleMessageView;

import java.util.Date;
import java.util.Locale;


public class MessageViewFragment extends SherlockFragment implements ConfirmationDialogFragmentListener {
    private static final boolean DEBUG = true;

    private static final String ARG_MSG_ID = "_id";

    public static MessageViewFragment newInstance(Integer msgId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": msgId " + msgId);
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_MSG_ID, msgId);
        fragment.setArguments(args);

        return fragment;
    }


    private SingleMessageView mMessageView;

    private Integer mDb_msg_Id;
    private Message mMessage;

    //private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();
    private LayoutInflater mLayoutInflater;

    private MessageViewFragmentListener mFragmentListener;

    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean mInitialized = false;

    private Context mContext;


    class MessageViewHandler extends Handler {

        public void progress(final boolean progress) {
            post(new Runnable() {
                @Override
                public void run() {
                    setProgress(progress);
                }
            });
        }
        /* A helper for a set of "show a toast" methods */
        private void showToast(final String message, final int toastLength)  {
            post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, toastLength).show();
                }
            });
        }

        public void networkError() {
            // FIXME: This is a hack. Fix the Handler madness!
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.status_network_error), Toast.LENGTH_LONG);
        }

        public void invalidIdError() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.status_invalid_id_error), Toast.LENGTH_LONG);
        }


        public void fetchingAttachment() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.message_view_fetching_attachment_toast), Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": savedInstance " + savedInstanceState);

        // This fragments adds options to the action bar
        setHasOptionsMenu(true);

        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        Context context = new ContextThemeWrapper(inflater.getContext(),
                K9.getK9ThemeResourceId(K9.getK9MessageViewTheme()));
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mLayoutInflater.inflate(R.layout.message, container, false);

        mMessageView = (SingleMessageView) view.findViewById(R.id.message_view);
        mMessageView.initialize(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        Bundle args = getArguments();
        displayMessage(args.getInt(ARG_MSG_ID));
    }

    private void displayMessage(Integer db_msg_id) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": db_msg_id " + db_msg_id);
        mDb_msg_Id = db_msg_id;

        // Clear previous message
        mMessageView.resetView();
        mMessageView.resetHeaderView();


        Uri itemUri = ContentUris.withAppendedId(MessagerProvider.CONTENT_URI, mDb_msg_Id);
        Cursor cursor = getActivity().getContentResolver().query(itemUri, null, null, null, null);
        if(cursor == null || !cursor.moveToFirst()) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                    + ": got invalid cursor. Skipping...");
            mFragmentListener.showHoldMessage();
            return;
        }

        //DIMA TODO: load Data from DB
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": set messages which should be shown!!!!!");
        String text = "<img src=\"http://www.w3schools.com/images/pulpit.jpg\" alt=\"Smiley face\" width=\"200\" height=\"200\">";
        mMessageView.showStatusMessage(text);
        //DIMA TODO: change getting data from cursor
        mMessageView.setHeaders(cursor.getString(1), "from", "to", new Date().getTime(), false, false, false);

        mMessageView.setOnFlagListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
                onToggleFlagged();
            }
        });

        mFragmentListener.updateMenu();
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        showDialog(R.id.dialog_confirm_delete);
    }

    private void delete() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mDb_msg_Id != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();

            Uri deleteUri = ContentUris.withAppendedId(MessagerProvider.CONTENT_URI, mDb_msg_Id);
            getActivity().getContentResolver().delete(deleteUri, null, null);
            //DIMA TODO: add removing selected msgId!!!! May be showHoldMessage wont be shown
            mFragmentListener.showHoldMessage();
        }
    }

    public void onRefile(String dstFolder) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /*
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (mAccount.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            mDstFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
        */
    }

    private void refileMessage(String dstFolder) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /*
        String srcFolder = mMessageReference.folderName;
        Message messageToMove = mMessage;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
        */
    }

    public void onReply() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage, null/*, mPgpData*/);
        }
    }

    public void onForward() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage, null/*, mPgpData*/);
        }
    }

    public void onToggleFlagged() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /*DIMA TODO: called in case star has been selected
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
        */
    }

/* DIMA TODO: does it called when folder must be change
    private void startRefileActivity(int activity) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }
*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        /*if (mAccount.getCryptoProvider().onDecryptActivityResult(this, requestCode, resultCode, data, mPgpData)) {
            return;
        }*/

        if (resultCode != Activity.RESULT_OK) {
            return;
        }
/*
        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            attachmentTmpStore.writeFile(new File(filePath));
                        }
                    }
                }
                break;
            }
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
                if (mMessageReference.equals(ref)) {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            mFragmentListener.showNextMessageOrReturn();
                            moveMessage(ref, destFolderName);
                            break;
                        }
                        case ACTIVITY_CHOOSE_FOLDER_COPY: {
                            copyMessage(ref, destFolderName);
                            break;
                        }
                    }
                }
                break;
            }
        }*/
    }

    public void onToggleRead() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /*
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
        */
    }

/*
    @Override
    public void onClick(View view) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": ERROR ENTERING");

        if (view.getId() == R.id.download) {
                ((AttachmentView)view).saveFile();
        }
        else if (view.getId() == R.id.download_remainder) {
                onDownloadRemainder();
        }
         DIMA: Change for using in library
        switch (view.getId()) {
            case R.id.download: {
                ((AttachmentView)view).saveFile();
                break;
            }
            case R.id.download_remainder: {
                onDownloadRemainder();
                break;
            }
        }
    }
*/
    private void setProgress(boolean enable) {
        if (mFragmentListener != null) {
            mFragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mFragmentListener != null) {
            mFragmentListener.displayMessageSubject(subject);
        }
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewHeadersAvailable(final Account account, String folder, String uid,
                final Message message) {
           /* if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }*/

            /*
             * Clone the message object because the original could be modified by
             * MessagingController later. This could lead to a ConcurrentModificationException
             * when that same object is accessed by the UI thread (below).
             *
             * See issue 3953
             *
             * This is just an ugly hack to get rid of the most pressing problem. A proper way to
             * fix this is to make Message thread-safe. Or, even better, rewriting the UI code to
             * access messages via a ContentProvider.
             *
             */
            final Message clonedMessage = message.clone();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
                    if (!clonedMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !clonedMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        String text = mContext.getString(R.string.message_view_downloading);
                        mMessageView.showStatusMessage(text);
                    }
                    mMessageView.setHeaders(clonedMessage, account);
                    final String subject = clonedMessage.getSubject();
                    if (subject == null || subject.equals("")) {
                        displayMessageSubject(mContext.getString(R.string.general_no_subject));
                    } else {
                        displayMessageSubject(clonedMessage.getSubject());
                    }
                    mMessageView.setOnFlagListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
                            onToggleFlagged();
                        }
                    });
                }
            });
        }

        @Override
        public void loadMessageForViewBodyAvailable(final Account account, String folder,
                String uid, final Message message) {
          /*  if (!mMessageReference.uid.equals(uid) ||
                    !mMessageReference.folderName.equals(folder) ||
                    !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }*/

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
                    /*
                    try {
                        mMessage = message;
                        mMessageView.setMessage(account, (LocalMessage) message, mPgpData,
                                mController, mListener);
                       */ mFragmentListener.updateMenu();
/*
                    } catch (MessagingException e) {
                        Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                    */

                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, final Throwable t) {
           /* if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }*/
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(false);
                    if (t instanceof IllegalArgumentException) {
                        mHandler.invalidIdError();
                    } else {
                        mHandler.networkError();
                    }
                    if (mMessage == null || mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        mMessageView.showStatusMessage(
                                mContext.getString(R.string.webview_empty_message));
                    }
                }
            });
        }


        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, final Message message) {
         /*   if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }*/
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(false);
                }
            });
        }


        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
        /*    if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }*/
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message, Part part, Object tag, final boolean requiresDownload) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showDialog(R.id.dialog_attachment_progress);
                    if (requiresDownload) {
                        mHandler.fetchingAttachment();
                    }
                }
            });
        }

        @Override
        public void loadAttachmentFinished(Account account, Message message, Part part, final Object tag) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                    + ": ERROR ENTERING");
                    /*mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    Object[] params = (Object[]) tag;
                    boolean download = (Boolean) params[0];
                    AttachmentView attachment = (AttachmentView) params[1];
                    if (download) {
                        attachment.writeFile();
                    } else {
                        attachment.showFile();
                    }*/
                }
            });
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part, Object tag, String reason) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    removeDialog(R.id.dialog_attachment_progress);
                    mHandler.networkError();
                }
            });
        }
    }


/**
 * Used by MessageOpenPgpView
 */
/*public void setMessageWithOpenPgp(String decryptedData, OpenPgpSignatureResult signatureResult) {
    if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

    try {
        // TODO: get rid of PgpData?
        PgpData data = new PgpData();
        data.setDecryptedData(decryptedData);
        data.setSignatureResult(signatureResult);
        mMessageView.setMessage(mAccount, (LocalMessage) mMessage, data, mController, mListener);
    } catch (MessagingException e) {
        if (DEBUG) Log.d(K9.LOG_TAG, "displayMessageBody failed", e);
    }

}

// This REALLY should be in MessageCryptoView
@Override
public void onDecryptDone(PgpData pgpData) {

    Account account = mAccount;
    LocalMessage message = (LocalMessage) mMessage;
    MessagingController controller = mController;
    Listener listener = mListener;
    try {
        mMessageView.setMessage(account, message, pgpData, controller, listener);
    } catch (MessagingException e) {
        if (DEBUG) Log.d(K9.LOG_TAG, "displayMessageBody failed", e);
    }
}
*/

    private void showDialog(int dialogId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        DialogFragment fragment;

        if (dialogId == R.id.dialog_confirm_delete) {
                String title = getString(R.string.dialog_confirm_delete_title);
                String message = getString(R.string.dialog_confirm_delete_message);
                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
        }/*
        else if (dialogId == R.id.dialog_confirm_spam) {
                String title = getString(R.string.dialog_confirm_spam_title);
                String message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1);
                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
        }
        else if (dialogId == R.id.dialog_attachment_progress) {
            String message = getString(R.string.dialog_attachment_progress_title);
            fragment = ProgressDialogFragment.newInstance(null, message);
        }*/
        else {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private void removeDialog(int dialogId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        FragmentManager fm = getFragmentManager();

        if (fm == null || isRemoving() || isDetached()) {
            return;
        }

        // Make sure the "show dialog" transaction has been processed when we call
        // findFragmentByTag() below. Otherwise the fragment won't be found and the dialog will
        // never be dismissed.
        fm.executePendingTransactions();

        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(getDialogTag(dialogId));

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private String getDialogTag(int dialogId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return String.format(Locale.US, "dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mMessageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (dialogId == R.id.dialog_confirm_delete) {
            delete();
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /* do nothing */
    }

    @Override
    public void dialogCancelled(int dialogId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /* do nothing */
    }

    /**
     * Get the {@link java.lang.Integer} of the currently displayed message.
     */
    public Integer getMsgId() {
        return mDb_msg_Id;
    }

    public boolean isMessageRead() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return (mMessage != null) ? mMessage.isSet(Flag.SEEN) : false;
    }
/*
    public boolean isCopyCapable() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; //mController.isCopyCapable(mAccount);
    }

    public boolean isMoveCapable() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; //mController.isMoveCapable(mAccount);
    }

    public boolean canMessageBeArchived() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return (!mMessageReference.folderName.equals(mAccount.getArchiveFolderName())
                && mAccount.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return (!mMessageReference.folderName.equals(mAccount.getSpamFolderName())
                && mAccount.hasSpamFolder());
    }

    public void updateTitle() {
        if (mMessage != null) {
            displayMessageSubject(mMessage.getSubject());
        }
    }
*/

    public interface MessageViewFragmentListener {
        public void onForward(Message mMessage, PgpData mPgpData);
        public void disableDeleteAction();
        public void onReplyAll(Message mMessage, PgpData mPgpData);
        public void onReply(Message mMessage, PgpData mPgpData);
        public void displayMessageSubject(String title);
        public void setProgress(boolean b);
        public void showNextMessageOrReturn();
        public void showHoldMessage();
        public void messageHeaderViewAvailable(MessageHeader messageHeaderView);
        public void updateMenu();
    }

    public boolean isInitialized() {
        return mInitialized ;
    }

    public LayoutInflater getFragmentLayoutInflater() {
        return mLayoutInflater;
    }
}
