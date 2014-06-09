package com.fsck.k9.view;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.helper.ClipboardManager;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

import java.util.List;


public class SingleMessageView extends LinearLayout implements /*OnClickListener,*/
        MessageHeader.OnLayoutChangedListener, OnCreateContextMenuListener {
    private static final boolean DEBUG = true;

/*
private static final int MENU_ITEM_LINK_VIEW = Menu.FIRST;
private static final int MENU_ITEM_LINK_SHARE = Menu.FIRST + 1;
private static final int MENU_ITEM_LINK_COPY = Menu.FIRST + 2;

private static final int MENU_ITEM_IMAGE_VIEW = Menu.FIRST;
private static final int MENU_ITEM_IMAGE_SAVE = Menu.FIRST + 1;
private static final int MENU_ITEM_IMAGE_COPY = Menu.FIRST + 2;

private static final int MENU_ITEM_PHONE_CALL = Menu.FIRST;
private static final int MENU_ITEM_PHONE_SAVE = Menu.FIRST + 1;
private static final int MENU_ITEM_PHONE_COPY = Menu.FIRST + 2;

private static final int MENU_ITEM_EMAIL_SEND = Menu.FIRST;
private static final int MENU_ITEM_EMAIL_SAVE = Menu.FIRST + 1;
private static final int MENU_ITEM_EMAIL_COPY = Menu.FIRST + 2;

private static final String[] ATTACHMENT_PROJECTION = new String[] {
    AttachmentProviderColumns._ID,
    AttachmentProviderColumns.DISPLAY_NAME
};
    private static final int DISPLAY_NAME_INDEX = 1;
*/



    private MessageWebView mMessageContentView;
    private MessageHeader mHeaderContainer;

    private boolean mShowPictures;
    private LayoutInflater mInflater;
    private ClipboardManager mClipboardManager;
    private String mText;


    public void initialize(Fragment fragment) {
        Activity activity = fragment.getActivity();

        //Header
        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        mHeaderContainer.setOnLayoutChangedListener(this);

        //Message body
        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);
        mMessageContentView.configure();
        activity.registerForContextMenu(mMessageContentView);
        mMessageContentView.setOnCreateContextMenuListener(this);


        setLoadPictures(true);

        mInflater = ((MessageViewFragment) fragment).getFragmentLayoutInflater();
        mMessageContentView.setVisibility(View.VISIBLE);

        // the HTC version of WebView tries to force the background of the
        // titlebar, which is really unfair.
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.messageViewHeaderBackgroundColor, outValue, true);
        mHeaderContainer.setBackgroundColor(outValue.data);
        // also set background of the whole view (including the attachments view)
        setBackgroundColor(outValue.data);
        mClipboardManager = ClipboardManager.getInstance(activity);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu);

        WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();

        if (result == null) {
            return;
        }

        int type = result.getType();
        Context context = getContext();

       /* switch (type) { DIMA TODO: That comment disconnect clicking of message body for doing some actions like open image in browser or save it
            case HitTestResult.SRC_ANCHOR_TYPE: {
                final String url = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (DEBUG) if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                        + ": item " + item);
                        switch (item.getItemId()) {
                            case MENU_ITEM_LINK_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_LINK_SHARE: {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_TEXT, url);
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_LINK_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_link_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle(url);

                menu.add(Menu.NONE, MENU_ITEM_LINK_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_link_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_SHARE, 1,
                        context.getString(R.string.webview_contextmenu_link_share_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_COPY, 2,
                        context.getString(R.string.webview_contextmenu_link_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case HitTestResult.IMAGE_TYPE:
            case HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                final String url = result.getExtra();
                final boolean externalImage = url.startsWith("http");
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (DEBUG) if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                        + ": item " + item);
                        switch (item.getItemId()) {
                            case MENU_ITEM_IMAGE_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                if (!externalImage) {
                                    // Grant read permission if this points to our
                                    // AttachmentProvider
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_IMAGE_SAVE: {
                                new DownloadImageTask().execute(url);
                                break;
                            }
                            case MENU_ITEM_IMAGE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_image_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle((externalImage) ?
                        url : context.getString(R.string.webview_contextmenu_image_title));

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_image_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_SAVE, 1,
                        (externalImage) ?
                            context.getString(R.string.webview_contextmenu_image_download_action) :
                            context.getString(R.string.webview_contextmenu_image_save_action))
                        .setOnMenuItemClickListener(listener);

                if (externalImage) {
                    menu.add(Menu.NONE, MENU_ITEM_IMAGE_COPY, 2,
                            context.getString(R.string.webview_contextmenu_image_copy_action))
                            .setOnMenuItemClickListener(listener);
                }

                break;
            }
            case HitTestResult.PHONE_TYPE: {
                final String phoneNumber = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (DEBUG) if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                        + ": item " + item);
                        switch (item.getItemId()) {
                            case MENU_ITEM_PHONE_CALL: {
                                Uri uri = Uri.parse(WebView.SCHEME_TEL + phoneNumber);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_PHONE_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.addPhoneContact(phoneNumber);
                                break;
                            }
                            case MENU_ITEM_PHONE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_phone_clipboard_label);
                                mClipboardManager.setText(label, phoneNumber);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(phoneNumber);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_CALL, 0,
                        context.getString(R.string.webview_contextmenu_phone_call_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_phone_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_COPY, 2,
                        context.getString(R.string.webview_contextmenu_phone_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case WebView.HitTestResult.EMAIL_TYPE: {
                final String email = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (DEBUG) if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                        + ": item " + item);
                        switch (item.getItemId()) {
                            case MENU_ITEM_EMAIL_SEND: {
                                Uri uri = Uri.parse(WebView.SCHEME_MAILTO + email);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivityIfAvailable(getContext(), intent);
                                break;
                            }
                            case MENU_ITEM_EMAIL_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.createContact(new Address(email));
                                break;
                            }
                            case MENU_ITEM_EMAIL_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_email_clipboard_label);
                                mClipboardManager.setText(label, email);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(email);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SEND, 0,
                        context.getString(R.string.webview_contextmenu_email_send_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_email_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_COPY, 2,
                        context.getString(R.string.webview_contextmenu_email_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
        }*/
    }

    private void startActivityIfAvailable(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_LONG).show();
        }
    }
/*
    @Override
    public void onClick(View view) {
        if (DEBUG) if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
            + ": ERROR ENTERING");*/

    /*    if (view.getId() == R.id.show_hidden_attachments) {
            onShowHiddenAttachments();
        }
        else if (view.getId() == R.id.show_message) {
                onShowMessage();
        }
        else if (view.getId() == R.id.show_attachments) {
                onShowAttachments();
        }
        else if (view.getId() == R.id.show_pictures) {
                // Allow network access first...
                setLoadPictures(true);
                // ...then re-populate the WebView with the message text
                loadBodyFromText(mText);
        }*/
   // }


    public SingleMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    private boolean isScreenReaderActive(Activity activity) {
        final String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
        final String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = activity.getPackageManager().queryIntentServices(
                                              screenReaderIntent, 0);
        ContentResolver cr = activity.getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders) {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                                        + ".providers.StatusProvider"), null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    // These content providers use a special cursor that only has
                    // one element,
                    // an integer that is 1 if the screen reader is running.
                    status = cursor.getInt(0);
                    if (status == 1) {
                        return true;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return false;
    }

    public boolean showPictures() {
        return mShowPictures;
    }

    public void setShowPictures(Boolean show) {
        mShowPictures = show;
    }

    /**
     * Enable/disable image loading of the WebView. But always hide the
     * "Show pictures" button!
     *
     * @param enable true, if (network) images should be loaded.
     *               false, otherwise.
     */
    public void setLoadPictures(boolean enable) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": value " + enable);
        mMessageContentView.blockNetworkData(!enable);
        setShowPictures(enable);
    }

    /*
    public Button downloadRemainderButton() {
        return  mDownloadRemainder;
    }

    public void showShowPicturesAction(boolean show) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": show " + show);
        mShowPicturesAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    public void showShowMessageAction(boolean show) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": show " + show);
        mShowMessageAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    public void showShowAttachmentsAction(boolean show) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": show " + show);
        mShowAttachmentsAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    */

    /**
     * Fetch the message header view.  This is not the same as the message headers; this is the View shown at the top
     * of messages.
     * @return MessageHeader View.
     */
    public MessageHeader getMessageHeaderView() {
        return mHeaderContainer;
    }

    public void setDefaultHeaders() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mHeaderContainer.setVisibility(View.VISIBLE);
    }

    public void setHeaders(String subject, String from, String to, long date, boolean isFlagged,
                           boolean isAnswered, boolean isForwarded) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + "2");
        mHeaderContainer.populate(subject, from, to, date, isFlagged, isAnswered, isForwarded);
        mHeaderContainer.setVisibility(View.VISIBLE);
    }

    public void setHeaders(final Message message, Account account) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        try {
            mHeaderContainer.populate(message, account);
            mHeaderContainer.setVisibility(View.VISIBLE);


        } catch (Exception me) {
            if (DEBUG) Log.d(K9.LOG_TAG, "setHeaders - error", me);
        }
    }

    public void setOnFlagListener(OnClickListener listener) {
        mHeaderContainer.setOnFlagListener(listener);
    }

    public void setMessage(/*Account account, LocalMessage message, PgpData pgpData,
            MessagingController controller, MessagingListener listener*/
           String msg_text, boolean hasAttachments) throws MessagingException {
        resetView();


        // Save the text so we can reset the WebView when the user clicks the "Show pictures" button
        mText = msg_text;
/*
        mHasAttachments = hasAttachments;

        if (mHasAttachments) {
            if (DEBUG) Log.d("DIMA","implement attachments loading if need. Otherwise - remove");
          //  renderAttachments(message, 0, message, account, controller, listener);
        }


        boolean lookForImages = true;
        if (mSavedState != null) {
            if (mSavedState.showPictures) {
                setLoadPictures(true);
                lookForImages = false;
            }

            if (mSavedState.attachmentViewVisible) {
                onShowAttachments();
            } else {
                onShowMessage();
            }

            if (mSavedState.hiddenAttachmentsVisible) {
                onShowHiddenAttachments();
            }

            mSavedState = null;
        } else {
            onShowMessage();
        }

        if (msg_text != null && lookForImages) {
            // If the message contains external pictures and the "Show pictures"
            // button wasn't already pressed, see if the user's preferences has us
            // showing them anyway.
            if (Utility.hasExternalImages(msg_text) && !showPictures()) {
                Address[] from = message.getFrom();
                if ((account.getShowPictures() == Account.ShowPictures.ALWAYS) ||
                        ((account.getShowPictures() == Account.ShowPictures.ONLY_FROM_CONTACTS) &&
                         // Make sure we have at least one from address
                         (from != null && from.length > 0) &&
                         mContacts.isInContacts(from[0].getAddress()))) {
                    setLoadPictures(true);
                } else {
                    showShowPicturesAction(true);
                //}
            }
        }
*/
        if (msg_text != null) {
            loadBodyFromText(msg_text);
        } else {
            showStatusMessage(getContext().getString(R.string.webview_empty_message));
        }
    }

    public void showStatusMessage(String status) {
        String text = "<div style=\"text-align:center; color: grey;\">" + status + "</div>";
        loadBodyFromText(text);
    }

    private void loadBodyFromText(String emailText) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": emailText '" + emailText + "'");
        mMessageContentView.setText(emailText);
    }

    public void zoom(KeyEvent event) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": event shift pressed " + event.isShiftPressed());

        if (event.isShiftPressed()) {
            mMessageContentView.zoomIn();
        } else {
            mMessageContentView.zoomOut();
        }
    }

    public void beginSelectingText() {
        mMessageContentView.emulateShiftHeld();
    }

    public void resetView() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        loadBodyFromText("");
    }

    public void resetHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public void setAttachmentCallback(
        AttachmentView.AttachmentFileDownloadCallback attachmentCallback) {
        if (DEBUG) if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": ERROR ENTERING");
        /*
        this.attachmentCallback = attachmentCallback;
        */
    }

    /*
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);

        savedState.attachmentViewVisible = (mAttachmentsContainer != null &&
                mAttachmentsContainer.getVisibility() == View.VISIBLE);
        savedState.hiddenAttachmentsVisible = (mHiddenAttachments != null &&
                mHiddenAttachments.getVisibility() == View.VISIBLE);
        savedState.showPictures = mShowPictures;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mSavedState = savedState;
    }
    */

    @Override
    public void onLayoutChanged() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mMessageContentView != null) {
            mMessageContentView.invalidate();
        }
    }
/*
    static class SavedState extends BaseSavedState {
        boolean attachmentViewVisible;
        boolean hiddenAttachmentsVisible;
        boolean showPictures;

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.attachmentViewVisible = (in.readInt() != 0);
            this.hiddenAttachmentsVisible = (in.readInt() != 0);
            this.showPictures = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.attachmentViewVisible) ? 1 : 0);
            out.writeInt((this.hiddenAttachmentsVisible) ? 1 : 0);
            out.writeInt((this.showPictures) ? 1 : 0);
        }
    }

    class DownloadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            try {
                boolean externalImage = urlString.startsWith("http");

                String filename = null;
                String mimeType = null;
                InputStream in = null;

                try {
                    if (externalImage) {
                        URL url = new URL(urlString);
                        URLConnection conn = url.openConnection();
                        in = conn.getInputStream();

                        String path = url.getPath();

                        // Try to get the filename from the URL
                        int start = path.lastIndexOf("/");
                        if (start != -1 && start + 1 < path.length()) {
                            filename = URLDecoder.decode(path.substring(start + 1), "UTF-8");
                        } else {
                            // Use a dummy filename if necessary
                            filename = "saved_image";
                        }

                        // Get the MIME type if we couldn't find a file extension
                        if (filename.indexOf('.') == -1) {
                            mimeType = conn.getContentType();
                        }
                    } else {
                        ContentResolver contentResolver = getContext().getContentResolver();
                        Uri uri = Uri.parse(urlString);

                        // Get the filename from AttachmentProvider
                        Cursor cursor = contentResolver.query(uri, ATTACHMENT_PROJECTION, null, null, null);
                        if (cursor != null) {
                            try {
                                if (cursor.moveToNext()) {
                                    filename = cursor.getString(DISPLAY_NAME_INDEX);
                                }
                            } finally {
                                cursor.close();
                            }
                        }

                        // Use a dummy filename if necessary
                        if (filename == null) {
                            filename = "saved_image";
                        }

                        // Get the MIME type if we couldn't find a file extension
                        if (filename.indexOf('.') == -1) {
                            mimeType = contentResolver.getType(uri);
                        }

                        in = contentResolver.openInputStream(uri);
                    }

                    // Do we still need an extension?
                    if (filename.indexOf('.') == -1) {
                        // Use JPEG as fallback
                        String extension = "jpeg";
                        if (mimeType != null) {
                            // Try to find an extension for the given MIME type
                            String ext = MimeUtility.getExtensionByMimeType(mimeType);
                            if (ext != null) {
                                extension = ext;
                            }
                        }
                        filename += "." + extension;
                    }

                    String sanitized = Utility.sanitizeFilename(filename);

                    File directory = new File(K9.getAttachmentDefaultPath());
                    File file = Utility.createUniqueFile(directory, sanitized);
                    FileOutputStream out = new FileOutputStream(file);
                    try {
                        IOUtils.copy(in, out);
                        out.flush();
                    } finally {
                        out.close();
                    }

                    return file.getName();

                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(String filename) {
            String text;
            if (filename == null) {
                text = getContext().getString(R.string.image_saving_failed);
            } else {
                text = getContext().getString(R.string.image_saved_as, filename);
            }

            Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
        }
    }
*/
}
