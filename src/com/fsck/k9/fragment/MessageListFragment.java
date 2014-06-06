package com.fsck.k9.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.ActivityListener;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageList.SortType;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.misc.ContactPictureLoader;
import com.fsck.k9.cache.EmailProviderCache;
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.MessageColumns;
import com.fsck.k9.provider.EmailProvider.SpecialColumns;
import com.fsck.k9.provider.EmailProvider.ThreadColumns;
import com.fsck.k9.provider.MessagerProvider;
import com.fsck.k9.search.LocalSearch;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class MessageListFragment extends SherlockFragment implements OnItemClickListener,
        ConfirmationDialogFragmentListener, LoaderCallbacks<Cursor> {


    private static final String[] THREADED_PROJECTION = {
        MessageColumns.ID,
        MessageColumns.UID,
        MessageColumns.INTERNAL_DATE,
        MessageColumns.SUBJECT,
        MessageColumns.DATE,
        MessageColumns.SENDER_LIST,
        MessageColumns.TO_LIST,
        MessageColumns.CC_LIST,
        MessageColumns.READ,
        MessageColumns.FLAGGED,
        MessageColumns.ANSWERED,
        MessageColumns.FORWARDED,
        MessageColumns.ATTACHMENT_COUNT,
        MessageColumns.FOLDER_ID,
        MessageColumns.PREVIEW,
        ThreadColumns.ROOT,
        SpecialColumns.ACCOUNT_UUID,
        SpecialColumns.FOLDER_NAME,

        SpecialColumns.THREAD_COUNT,
    };

    private static final int ID_COLUMN = 0;
    private static final int UID_COLUMN = 1;
    private static final int INTERNAL_DATE_COLUMN = 2;
    private static final int SUBJECT_COLUMN = 3;
    private static final int DATE_COLUMN = 4;
    private static final int SENDER_LIST_COLUMN = 5;
    private static final int TO_LIST_COLUMN = 6;
    private static final int CC_LIST_COLUMN = 7;
    private static final int READ_COLUMN = 8;
    private static final int FLAGGED_COLUMN = 9;
    private static final int ANSWERED_COLUMN = 10;
    private static final int FORWARDED_COLUMN = 11;
    private static final int ATTACHMENT_COUNT_COLUMN = 12;
    private static final int FOLDER_ID_COLUMN = 13;
    private static final int PREVIEW_COLUMN = 14;
    private static final int THREAD_ROOT_COLUMN = 15;
    private static final int ACCOUNT_UUID_COLUMN = 16;
    private static final int FOLDER_NAME_COLUMN = 17;
    private static final int THREAD_COUNT_COLUMN = 18;

    private static final String[] PROJECTION = Utility.copyOf(THREADED_PROJECTION,
            THREAD_COUNT_COLUMN);



    public static MessageListFragment newInstance() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        MessageListFragment fragment = new MessageListFragment();
        return fragment;
    }

    /**
     * Reverses the result of a {@link Comparator}.
     *
     * @param <T>
     */
    public static class ReverseComparator<T> implements Comparator<T> {
        private Comparator<T> mDelegate;

        /**
         * @param delegate
         *         Never {@code null}.
         */
        public ReverseComparator(final Comparator<T> delegate) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            mDelegate = delegate;
        }

        @Override
        public int compare(final T object1, final T object2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            // arg1 & 2 are mixed up, this is done on purpose
            return mDelegate.compare(object2, object1);
        }
    }

    /**
     * Chains comparator to find a non-0 result.
     *
     * @param <T>
     */
    public static class ComparatorChain<T> implements Comparator<T> {
        private List<Comparator<T>> mChain;

        /**
         * @param chain
         *         Comparator chain. Never {@code null}.
         */
        public ComparatorChain(final List<Comparator<T>> chain) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            mChain = chain;
        }

        @Override
        public int compare(T object1, T object2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            int result = 0;
            for (final Comparator<T> comparator : mChain) {
                result = comparator.compare(object1, object2);
                if (result != 0) {
                    break;
                }
            }
            return result;
        }
    }

    public static class ReverseIdComparator implements Comparator<Cursor> {
        private int mIdColumn = -1;

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if (mIdColumn == -1) {
                mIdColumn = cursor1.getColumnIndex("_id");
            }
            long o1Id = cursor1.getLong(mIdColumn);
            long o2Id = cursor2.getLong(mIdColumn);
            return (o1Id > o2Id) ? -1 : 1;
        }
    }

    public static class AttachmentComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            int o1HasAttachment = (cursor1.getInt(ATTACHMENT_COUNT_COLUMN) > 0) ? 0 : 1;
            int o2HasAttachment = (cursor2.getInt(ATTACHMENT_COUNT_COLUMN) > 0) ? 0 : 1;
            return o1HasAttachment - o2HasAttachment;
        }
    }

    public static class FlaggedComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            int o1IsFlagged = (cursor1.getInt(FLAGGED_COLUMN) == 1) ? 0 : 1;
            int o2IsFlagged = (cursor2.getInt(FLAGGED_COLUMN) == 1) ? 0 : 1;
            return o1IsFlagged - o2IsFlagged;
        }
    }

    public static class UnreadComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            int o1IsUnread = cursor1.getInt(READ_COLUMN);
            int o2IsUnread = cursor2.getInt(READ_COLUMN);
            return o1IsUnread - o2IsUnread;
        }
    }

    public static class DateComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            long o1Date = cursor1.getLong(DATE_COLUMN);
            long o2Date = cursor2.getLong(DATE_COLUMN);
            if (o1Date < o2Date) {
                return -1;
            } else if (o1Date == o2Date) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public static class ArrivalComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            long o1Date = cursor1.getLong(INTERNAL_DATE_COLUMN);
            long o2Date = cursor2.getLong(INTERNAL_DATE_COLUMN);
            if (o1Date == o2Date) {
                return 0;
            } else if (o1Date < o2Date) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public static class SubjectComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            String subject1 = cursor1.getString(SUBJECT_COLUMN);
            String subject2 = cursor2.getString(SUBJECT_COLUMN);

            if (subject1 == null) {
                return (subject2 == null) ? 0 : -1;
            } else if (subject2 == null) {
                return 1;
            }

            return subject1.compareToIgnoreCase(subject2);
        }
    }

    public static class SenderComparator implements Comparator<Cursor> {

        @Override
        public int compare(Cursor cursor1, Cursor cursor2) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            String sender1 = getSenderAddressFromCursor(cursor1);
            String sender2 = getSenderAddressFromCursor(cursor2);

            if (sender1 == null && sender2 == null) {
                return 0;
            } else if (sender1 == null) {
                return 1;
            } else if (sender2 == null) {
                return -1;
            } else {
                return sender1.compareToIgnoreCase(sender2);
            }
        }
    }

/*
private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
*/
    private static final String STATE_ACTIVE_MESSAGE = "activeMsgId";

    /**
     * Maps a {@link SortType} to a {@link Comparator} implementation.
     */
    private static final Map<SortType, Comparator<Cursor>> SORT_COMPARATORS;

    static {
        // fill the mapping at class time loading

        final Map<SortType, Comparator<Cursor>> map =
                new EnumMap<SortType, Comparator<Cursor>>(SortType.class);
        map.put(SortType.SORT_ATTACHMENT, new AttachmentComparator());
        map.put(SortType.SORT_DATE, new DateComparator());
        map.put(SortType.SORT_ARRIVAL, new ArrivalComparator());
        map.put(SortType.SORT_FLAGGED, new FlaggedComparator());
        map.put(SortType.SORT_SUBJECT, new SubjectComparator());
        map.put(SortType.SORT_SENDER, new SenderComparator());
        map.put(SortType.SORT_UNREAD, new UnreadComparator());

        // make it immutable to prevent accidental alteration (content is immutable already)
        SORT_COMPARATORS = Collections.unmodifiableMap(map);
    }

    private ListView mListView;
    private PullToRefreshListView mPullToRefreshView;
    private Parcelable mSavedListState;

    private int mPreviewLines = 0;


    private MessageListAdapter mAdapter;
    private View mFooterView;
    private ContentObserver mContentObserver;

    private LayoutInflater mInflater;


    //DIMA TODO: add showing unread messages
    private int mUnreadMessageCount = -1;

    /**
     * Stores the name of the folder that we want to open as soon as possible after load.
     */

    //DIMA TODO: add printing folder name where we are
    private String mTitle;
    private FolderInfoHolder mCurrentFolder;

    private MessageListHandler mHandler = new MessageListHandler(this);

    private SortType mSortType = SortType.SORT_DATE;
    private boolean mSortAscending = true;
    private boolean mSortDateAscending = false;
    private boolean mSenderAboveSubject = false;
    private boolean mCheckboxes = false;
    private boolean mStars = true;

    private int mSelectedCount = 0;
    //private Set<Long> mSelected = new HashSet<Long>();

    private FontSizes mFontSizes = K9.getFontSizes();

    private ActionMode mActionMode;

    //private ActionModeCallback mActionModeCallback = new ActionModeCallback();


    private MessageListFragmentListener mFragmentListener;

    private Context mContext;

    private boolean mLoaderJustInitialized;

    private Integer mActiveMsgId;

    /**
     * {@code true} after {@link #onCreate(Bundle)} was executed. Used in {@link #updateTitle()} to
     * make sure we don't access member variables before initialization is complete.
     */
    private boolean mInitialized = false;

    private ContactPictureLoader mContactsPictureLoader;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mCacheBroadcastReceiver;
    private IntentFilter mCacheIntentFilter;

    /**
     * Stores the unique ID of the message the context menu was opened for.
     *
     * We have to save this because the message list might change between the time the menu was
     * opened and when the user clicks on a menu item. When this happens the 'adapter position' that
     * is accessible via the {@code ContextMenu} object might correspond to another list item and we
     * would end up using/modifying the wrong message.
     *
     * The value of this field is {@code 0} when no context menu is currently open.
     */
    private long mContextMenuUniqueId = 0;


    /**
     * This class is used to run operations that modify UI elements in the UI thread.
     *
     * <p>We are using convenience methods that add a {@link android.os.Message} instance or a
     * {@link Runnable} to the message queue.</p>
     *
     * <p><strong>Note:</strong> If you add a method to this class make sure you don't accidentally
     * perform the operation in the calling thread.</p>
     */
    static class MessageListHandler extends Handler {
        private static final int ACTION_FOLDER_LOADING = 1;
        private static final int ACTION_REFRESH_TITLE = 2;
        private static final int ACTION_PROGRESS = 3;
        private static final int ACTION_REMOTE_SEARCH_FINISHED = 4;
        private static final int ACTION_GO_BACK = 5;
        private static final int ACTION_RESTORE_LIST_POSITION = 6;
        private static final int ACTION_OPEN_MESSAGE = 7;

        private WeakReference<MessageListFragment> mFragment;

        public MessageListHandler(MessageListFragment fragment) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            mFragment = new WeakReference<MessageListFragment>(fragment);
        }
        public void folderLoading(String folder, boolean loading) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            android.os.Message msg = android.os.Message.obtain(this, ACTION_FOLDER_LOADING,
                    (loading) ? 1 : 0, 0, folder);
            sendMessage(msg);
        }

        public void refreshTitle() {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            android.os.Message msg = android.os.Message.obtain(this, ACTION_REFRESH_TITLE);
            sendMessage(msg);
        }

        public void progress(final boolean progress) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            android.os.Message msg = android.os.Message.obtain(this, ACTION_PROGRESS,
                    (progress) ? 1 : 0, 0);
            sendMessage(msg);
        }

        public void updateFooter(final String message) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            post(new Runnable() {
                @Override
                public void run() {
                    MessageListFragment fragment = mFragment.get();
                    if (fragment != null) {
                        fragment.updateFooter(message);
                    }
                }
            });
        }

        public void goBack() {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            android.os.Message msg = android.os.Message.obtain(this, ACTION_GO_BACK);
            sendMessage(msg);
        }

        public void restoreListPosition() {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            MessageListFragment fragment = mFragment.get();
            if (fragment != null) {
                android.os.Message msg = android.os.Message.obtain(this, ACTION_RESTORE_LIST_POSITION,
                        fragment.mSavedListState);
                fragment.mSavedListState = null;
                sendMessage(msg);
            }
        }

        public void openMessage(Integer id) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            android.os.Message msg = android.os.Message.obtain(this, ACTION_OPEN_MESSAGE,
                    id/*messageReference*/);
            sendMessage(msg);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            MessageListFragment fragment = mFragment.get();
            if (fragment == null) {
                return;
            }

            // The following messages don't need an attached activity.
            switch (msg.what) {
                case ACTION_REMOTE_SEARCH_FINISHED: {
                    fragment.remoteSearchFinished();
                    return;
                }
            }

            // Discard messages if the fragment isn't attached to an activity anymore.
            Activity activity = fragment.getActivity();
            if (activity == null) {
                return;
            }

            switch (msg.what) {
                case ACTION_FOLDER_LOADING: {
                    String folder = (String) msg.obj;
                    boolean loading = (msg.arg1 == 1);
                    fragment.folderLoading(folder, loading);
                    break;
                }
                case ACTION_REFRESH_TITLE: {
                    fragment.updateTitle();
                    break;
                }
                case ACTION_PROGRESS: {
                    boolean progress = (msg.arg1 == 1);
                    fragment.progress(progress);
                    break;
                }
                case ACTION_GO_BACK: {
                    fragment.mFragmentListener.goBack();
                    break;
                }
                case ACTION_RESTORE_LIST_POSITION: {
                    fragment.mListView.onRestoreInstanceState((Parcelable) msg.obj);
                    break;
                }
                case ACTION_OPEN_MESSAGE: {
                    Integer id = (Integer)msg.obj;
                    fragment.mFragmentListener.openMessage(id);
                    break;
                }
            }
        }
    }

    /**
     * @return The comparator to use to display messages in an ordered
     *         fashion. Never {@code null}.
     */
    protected Comparator<Cursor> getComparator() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        final List<Comparator<Cursor>> chain =
                new ArrayList<Comparator<Cursor>>(3 /* we add 3 comparators at most */);

        // Add the specified comparator
        final Comparator<Cursor> comparator = SORT_COMPARATORS.get(mSortType);
        if (mSortAscending) {
            chain.add(comparator);
        } else {
            chain.add(new ReverseComparator<Cursor>(comparator));
        }

        // Add the date comparator if not already specified
        if (mSortType != SortType.SORT_DATE && mSortType != SortType.SORT_ARRIVAL) {
            final Comparator<Cursor> dateComparator = SORT_COMPARATORS.get(SortType.SORT_DATE);
            if (mSortDateAscending) {
                chain.add(dateComparator);
            } else {
                chain.add(new ReverseComparator<Cursor>(dateComparator));
            }
        }

        // Add the id comparator
        chain.add(new ReverseIdComparator());

        // Build the comparator chain
        return new ComparatorChain<Cursor>(chain);
    }

    private void folderLoading(String folder, boolean loading) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mCurrentFolder != null && mCurrentFolder.equals(folder)) {
            mCurrentFolder.loading = loading;
        }
        updateFooterView();
    }

    public void updateTitle() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (!mInitialized) {
            return;
        }

        setWindowTitle();
    }

    private void setWindowProgress() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  int level = Window.PROGRESS_END;

        if (mCurrentFolder != null && mCurrentFolder.loading && mListener.getFolderTotal() > 0) {
            int divisor = mListener.getFolderTotal();
            if (divisor != 0) {
                level = (Window.PROGRESS_END / divisor) * (mListener.getFolderCompleted()) ;
                if (level > Window.PROGRESS_END) {
                    level = Window.PROGRESS_END;
                }
            }
        }

        mFragmentListener.setMessageListProgress(level);*/
    }

    private void setWindowTitle() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        mFragmentListener.setUnreadCount(-1);//mUnreadMessageCount);
    }

    private void progress(final boolean progress) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  mFragmentListener.enableActionBarProgress(progress);
        if (mPullToRefreshView != null && !progress) {
            mPullToRefreshView.onRefreshComplete();
        }*/
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": position " + position);

       /* if (view == mFooterView) {
            if (mCurrentFolder != null && !mSearch.isManualSearch()) {

                mController.loadMoreMessages(mAccount, mFolderName, null);

            } else if (mCurrentFolder != null && isRemoteSearch() &&
                    mExtraSearchResults != null && mExtraSearchResults.size() > 0) {

                int numResults = mExtraSearchResults.size();
                int limit = mAccount.getRemoteSearchNumResults();

                List<Message> toProcess = mExtraSearchResults;

                if (limit > 0 && numResults > limit) {
                    toProcess = toProcess.subList(0, limit);
                    mExtraSearchResults = mExtraSearchResults.subList(limit,
                            mExtraSearchResults.size());
                } else {
                    mExtraSearchResults = null;
                    updateFooter("");
                }

                mController.loadSearchResults(mAccount, mCurrentFolder.name, toProcess, mListener);
            }

            return;
        }

        Cursor cursor = (Cursor) parent.getItemAtPosition(position);
        if (cursor == null) {
            return;
        }

        if (mSelectedCount > 0) {
            toggleMessageSelect(position);
        } else {
            if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
                Account account = getAccountFromCursor(cursor);
                long folderId = cursor.getLong(FOLDER_ID_COLUMN);
                String folderName = getFolderNameById(account, folderId);

                // If threading is enabled and this item represents a thread, display the thread contents.
                long rootId = cursor.getLong(THREAD_ROOT_COLUMN);
                mFragmentListener.showThread(account, folderName, rootId);
            } else {
                // This item represents a message; just display the message.
                openMessageAtPosition(listViewToAdapterPosition(position));
            }
        }*/
        mHandler.openMessage(position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageListFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageListFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        Context appContext = getActivity().getApplicationContext();
/*
        mPreferences = Preferences.getPreferences(appContext);
        mController = MessagingController.getInstance(getActivity().getApplication());
*/
        mPreviewLines = K9.messageListPreviewLines();
        mCheckboxes = K9.messageListCheckboxes();
        K9.setMessageListStars(false); //DIMA : commented for disabling stars. Check if need
        mStars = K9.messageListStars();

        K9.setShowContactPicture(false); //DIMA : check if disabled pictures
        if (K9.showContactPicture()) {
            mContactsPictureLoader = ContactPicture.getContactPictureLoader(getActivity());
        }
        //mActiveMsgId = -1;

        restoreInstanceState(savedInstanceState);
        decodeArguments();

        createCacheBroadcastReceiver(appContext);

        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        mInflater = inflater;

        View view = inflater.inflate(R.layout.message_list_fragment, container, false);

        initializePullToRefresh(inflater, view);

        initializeLayout();

        mListView.setVerticalFadingEdgeEnabled(false);
        return view;
    }

    @Override
    public void onDestroyView() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mSavedListState = mListView.onSaveInstanceState();
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        mContentObserver = null;
        getLoaderManager().destroyLoader(0);

        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        initializeMessageList();

        // This needs to be done before initializing the cursor loader below
        initializeSortSettings();

        mLoaderJustInitialized = true;
        LoaderManager loaderManager = getLoaderManager();

        loaderManager.initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if(mActiveMsgId != null)
            outState.putInt(STATE_ACTIVE_MESSAGE, mActiveMsgId);
    }

    /**
     * Restore the state of a previous {@link MessageListFragment} instance.
     *
     * @see #onSaveInstanceState(Bundle)
     */
    private void restoreInstanceState(Bundle savedInstanceState) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (savedInstanceState == null) {
            return;
        }


        mActiveMsgId = savedInstanceState.getInt(STATE_ACTIVE_MESSAGE);

    }

    /**
     * Write the unique IDs of selected messages to a {@link Bundle}.
     */
    private void saveSelectedMessages(Bundle outState) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* long[] selected = new long[mSelected.size()];
        int i = 0;
        for (Long id : mSelected) {
            selected[i++] = id;
        }
        outState.putLongArray(STATE_SELECTED_MESSAGES, selected);*/
    }

    /**
     * Restore selected messages from a {@link Bundle}.
     */
    private void restoreSelectedMessages(Bundle savedInstanceState) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
  /*      long[] selected = savedInstanceState.getLongArray(STATE_SELECTED_MESSAGES);
        for (long id : selected) {
            mSelected.add(Long.valueOf(id));
        }*/
    }

    private void saveListState(Bundle outState) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    /*    if (mSavedListState != null) {
            // The previously saved state was never restored, so just use that.
            outState.putParcelable(STATE_MESSAGE_LIST, mSavedListState);
        } else if (mListView != null) {
            outState.putParcelable(STATE_MESSAGE_LIST, mListView.onSaveInstanceState());
        }*/
    }

    private void initializeSortSettings() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mSortAscending = true;
        mSortDateAscending = true;
       /* if (mSingleAccountMode) {
            mSortType = mAccount.getSortType();
            mSortAscending = mAccount.isSortAscending(mSortType);
            mSortDateAscending = mAccount.isSortAscending(SortType.SORT_DATE);
        } else {
            //mSortType = K9.getSortType();
            mSortAscending = K9.isSortAscending(mSortType);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);
        } */
    }

    private void decodeArguments() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        Bundle args = getArguments();

        /*mSearch = args.getParcelable(ARG_SEARCH);
        mTitle = mSearch.getName();

        String[] accountUuids = mSearch.getAccountUuids();

        if(true) { //DIMA hardcoded for 1 account
            Log.e("MessageListFragment", "decodeArguments: hardcoding account and folderName");
            mSingleAccountMode = true;
            mSingleFolderMode = true;
            mAccount = null;
            Log.e("MessageListFragment", "ERROR, account is NULL. Should add initialization!!!!");
            mFolderName = "allMessagesFolder";
            mAccountUuids = new String[] {"1"};
        }
        else {
            mSingleAccountMode = false;
            if (accountUuids.length == 1 && !mSearch.searchAllAccounts()) {
                mSingleAccountMode = true;
                //DIMA TODO: commented
                Log.e("ERROR", "decodeArguments: skipping");
                mAccount = mPreferences.getAccount(accountUuids[0]);
            }

            mSingleFolderMode = false;
            if (mSingleAccountMode && (mSearch.getFolderNames().size() == 1)) {
                mSingleFolderMode = true;
                mFolderName = mSearch.getFolderNames().get(0);
                mCurrentFolder = getFolder(mFolderName, mAccount);
            }

            mAllAccounts = false;
            if (mSingleAccountMode) {
                mAccountUuids = new String[] { mAccount.getUuid() };
            } else {
                if (accountUuids.length == 1 &&
                        accountUuids[0].equals(SearchSpecification.ALL_ACCOUNTS)) {
                    mAllAccounts = true;

                    Account[] accounts = mPreferences.getAccounts();

                    mAccountUuids = new String[accounts.length];
                    for (int i = 0, len = accounts.length; i < len; i++) {
                        mAccountUuids[i] = accounts[i].getUuid();
                    }

                    if (mAccountUuids.length == 1) {
                        mSingleAccountMode = true;
                        mAccount = accounts[0];
                    }
                } else {
                    mAccountUuids = accountUuids;
                }
            }
        }*/
    }

    private void initializeMessageList() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mAdapter = new MessageListAdapter();
/*
        if (mFolderName != null) {
            mCurrentFolder = getFolder(mFolderName, mAccount);
        }

        if (mSingleFolderMode) {*/
        mListView.addFooterView(getFooterView(mListView));
        updateFooterView();
        //}

        mListView.setAdapter(mAdapter);
        /*String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
                "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
                "Android", "iPhone", "WindowsMobile" };

        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_list_item_1, list);
        mListView.setAdapter(adapter);*/

    }

    private void createCacheBroadcastReceiver(Context appContext) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(appContext);

        mCacheBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAdapter.notifyDataSetChanged();
            }
        };

        mCacheIntentFilter = new IntentFilter(EmailProviderCache.ACTION_CACHE_UPDATED);
    }

    private FolderInfoHolder getFolder(String folder, Account account) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  LocalFolder localFolder = null;
        try {
            LocalStore localStore = account.getLocalStore();
            localFolder = localStore.getFolder(folder);
            return new FolderInfoHolder(mContext, localFolder, account);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "getFolder(" + folder + ") goes boom: ", e);
            return null;
        } finally {
            if (localFolder != null) {
                localFolder.close();
            }
        }*/
        return null;
    }

    private String getFolderNameById(Account account, long folderId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* try {
            Folder folder = getFolderById(account, folderId);
            if (folder != null) {
                return folder.getName();
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "getFolderNameById() failed.", e);
        }
*/
        return null;
    }

    private Folder getFolderById(Account account, long folderId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* try {
            LocalStore localStore = account.getLocalStore();
            LocalFolder localFolder = localStore.getFolderById(folderId);
            localFolder.open(Folder.OPEN_MODE_RO);
            return localFolder;
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "getFolderNameById() failed.", e);
            return null;
        }*/
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
/*
        mLocalBroadcastManager.unregisterReceiver(mCacheBroadcastReceiver);
        mListener.onPause(getActivity());
        mController.removeListener(mListener);*/
    }

    /**
     * On resume we refresh messages for the folder that is currently open.
     * This guarantees that things like unread message count and read status
     * are updated.
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        Context appContext = getActivity().getApplicationContext();

        mSenderAboveSubject = K9.messageListSenderAboveSubject();

        if (!mLoaderJustInitialized) {
            restartLoader();
        } else {
            mLoaderJustInitialized = false;
        }

        mLocalBroadcastManager.registerReceiver(mCacheBroadcastReceiver, mCacheIntentFilter);
        /*mListener.onResume(getActivity());
        mController.addListener(mListener);

        //Cancel pending new mail notifications when we open an account
        Account[] accountsWithNotification;

        Account account = mAccount;
        if (account != null) {
            accountsWithNotification = new Account[] { account };
        } else {
            accountsWithNotification = mPreferences.getAccounts();
        }

        for (Account accountWithNotification : accountsWithNotification) {
            mController.notifyAccountCancel(appContext, accountWithNotification);
        }

        if (mAccount != null && mFolderName != null && !mSearch.isManualSearch()) {
            mController.getFolderUnreadMessageCount(mAccount, mFolderName, mListener);
        }
*/
        updateTitle();

        if(mActiveMsgId != null)
            mHandler.openMessage(mActiveMsgId);
    }

    private void restartLoader() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.restartLoader(0, null, this);
    }

    private void initializePullToRefresh(LayoutInflater inflater, View layout) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mPullToRefreshView = (PullToRefreshListView) layout.findViewById(R.id.message_list);

        // Set empty view
        View loadingView = inflater.inflate(R.layout.message_list_loading, null);
        mPullToRefreshView.setEmptyView(loadingView);

        if (isRemoteSearchAllowed()) {
            // "Pull to search server"
            mPullToRefreshView.setOnRefreshListener(
                    new PullToRefreshBase.OnRefreshListener<ListView>() {
                        @Override
                        public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                            mPullToRefreshView.onRefreshComplete();
                            onRemoteSearchRequested();
                        }
                    });
            ILoadingLayout proxy = mPullToRefreshView.getLoadingLayoutProxy();
            proxy.setPullLabel(getString(
                    R.string.pull_to_refresh_remote_search_from_local_search_pull));
            proxy.setReleaseLabel(getString(
                    R.string.pull_to_refresh_remote_search_from_local_search_release));
        } else if (isCheckMailSupported()) {
            // "Pull to refresh"
            mPullToRefreshView.setOnRefreshListener(
                    new PullToRefreshBase.OnRefreshListener<ListView>() {
                @Override
                public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                    checkMail();
                }
            });
        }

        // Disable pull-to-refresh until the message list has been loaded
        setPullToRefreshEnabled(false);
    }

    /**
     * Enable or disable pull-to-refresh.
     *
     * @param enable
     *         {@code true} to enable. {@code false} to disable.
     */
    private void setPullToRefreshEnabled(boolean enable) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mPullToRefreshView.setMode((enable) ?
                PullToRefreshBase.Mode.PULL_FROM_START : PullToRefreshBase.Mode.DISABLED);
    }

    private void initializeLayout() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mListView = mPullToRefreshView.getRefreshableView();
        mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mListView.setLongClickable(true);
        mListView.setFastScrollEnabled(true);
        mListView.setScrollingCacheEnabled(false);
        mListView.setOnItemClickListener(this);

        registerForContextMenu(mListView);
    }

    public void onCompose() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
         mFragmentListener.onCompose();
    }

    public void onReply(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       // mFragmentListener.onReply(message);
    }

    public void onForward(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       // mFragmentListener.onForward(message);
    }

    public void onResendMessage(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       // mFragmentListener.onResendMessage(message);
    }

    public void changeSort(SortType sortType) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  Boolean sortAscending = (mSortType == sortType) ? !mSortAscending : null;
        changeSort(sortType, sortAscending);*/
    }

    /**
     * User has requested a remote search.  Setup the bundle and start the intent.
     */
    public void onRemoteSearchRequested() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   String searchAccount;
        String searchFolder;

        searchAccount = mAccount.getUuid();
        searchFolder = mCurrentFolder.name;

        String queryString = mSearch.getRemoteSearchArguments();

        mRemoteSearchPerformed = true;
        mRemoteSearchFuture = mController.searchRemoteMessages(searchAccount, searchFolder,
                queryString, null, null, mListener);

        setPullToRefreshEnabled(false);

        mFragmentListener.remoteSearchStarted();*/
    }

    /**
     * Change the sort type and sort order used for the message list.
     *
     * @param sortType
     *         Specifies which field to use for sorting the message list.
     * @param sortAscending
     *         Specifies the sort order. If this argument is {@code null} the default search order
     *         for the sort type is used.
     */
    // FIXME: Don't save the changes in the UI thread
    private void changeSort(SortType sortType, Boolean sortAscending) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* mSortType = sortType;

        Account account = mAccount;

        if (account != null) {
            account.setSortType(mSortType);

            if (sortAscending == null) {
                mSortAscending = account.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
            account.setSortAscending(mSortType, mSortAscending);
            mSortDateAscending = account.isSortAscending(SortType.SORT_DATE);

            account.save(mPreferences);
        } else {
            K9.setSortType(mSortType);

            if (sortAscending == null) {
                mSortAscending = K9.isSortAscending(mSortType);
            } else {
                mSortAscending = sortAscending;
            }
            K9.setSortAscending(mSortType, mSortAscending);
            mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);

            *//* DIMA TODO: #1
            Editor editor = mPreferences.getPreferences().edit();
            K9.save(editor);
            editor.commit();*//*
        }

        reSort();*/
    }

    private void reSort() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        int toastString = mSortType.getToast(mSortAscending);

        Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_SHORT);
        toast.show();

        LoaderManager loaderManager = getLoaderManager();
        /*for (int i = 0, len = mAccountUuids.length; i < len; i++) {
            loaderManager.restartLoader(i, null, this);
        }*/
        loaderManager.restartLoader(0, null, this);
    }

    public void onCycleSort() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        SortType[] sorts = SortType.values();
        int curIndex = 0;

        for (int i = 0; i < sorts.length; i++) {
            if (sorts[i] == mSortType) {
                curIndex = i;
                break;
            }
        }

        curIndex++;

        if (curIndex == sorts.length) {
            curIndex = 0;
        }

        changeSort(sorts[curIndex]);
    }

    private void onDelete(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  onDelete(Collections.singletonList(message));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
        case ACTIVITY_CHOOSE_FOLDER_MOVE:
        case ACTIVITY_CHOOSE_FOLDER_COPY: {
            if (data == null) {
                return;
            }

            final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
            final List<Message> messages = mActiveMessages;

            if (destFolderName != null) {

                mActiveMessages = null; // don't need it any more

                if (messages.size() > 0) {
                    Account account = messages.get(0).getFolder().getAccount();
                    account.setLastSelectedFolderName(destFolderName);
                }

                switch (requestCode) {
                case ACTIVITY_CHOOSE_FOLDER_MOVE:
                    move(messages, destFolderName);
                    break;

                case ACTIVITY_CHOOSE_FOLDER_COPY:
                    copy(messages, destFolderName);
                    break;
                }
            }
            break;
        }
        }*/
    }

    public void onExpunge() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   if (mCurrentFolder != null) {
            onExpunge(mAccount, mCurrentFolder.name);
        }*/
    }

    private void onExpunge(final Account account, String folderName) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  mController.expunge(account, folderName, null);
    }

    private void showDialog(int dialogId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  DialogFragment fragment;
        if(dialogId == R.id.dialog_confirm_spam) {
            String title = getString(R.string.dialog_confirm_spam_title);

            int selectionSize = mActiveMessages.size();
            String message = getResources().getQuantityString(
                    R.plurals.dialog_confirm_spam_message, selectionSize,
                    Integer.valueOf(selectionSize));

            String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
            String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

            fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                    confirmText, cancelText);
        }
        else {
            throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
        }
        *//* DIMA: Change for using in library
        switch (dialogId) {
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);

                int selectionSize = mActiveMessages.size();
                String message = getResources().getQuantityString(
                        R.plurals.dialog_confirm_spam_message, selectionSize,
                        Integer.valueOf(selectionSize));

                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);

                int selectionSize = mActiveMessages.size();
                String message = getResources().getQuantityString(
                        R.plurals.dialog_confirm_delete_messages, selectionSize,
                        Integer.valueOf(selectionSize));

                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }*//*

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));*/
    }

    private String getDialogTag(int dialogId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return "dialog-" + dialogId;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        int itemId = item.getItemId();
        if (itemId == R.id.set_sort_date) {
                changeSort(SortType.SORT_DATE);
                return true;
            }
        else if (itemId == R.id.set_sort_arrival) {
                changeSort(SortType.SORT_ARRIVAL);
                return true;
            }
        else if (itemId == R.id.set_sort_subject) {
                changeSort(SortType.SORT_SUBJECT);
                return true;
            }
        else if (itemId == R.id.set_sort_sender) {
                changeSort(SortType.SORT_SENDER);
                return true;
            }
        else if (itemId == R.id.set_sort_flag) {
                changeSort(SortType.SORT_FLAGGED);
                return true;
            }
        else if (itemId == R.id.set_sort_unread) {
                changeSort(SortType.SORT_UNREAD);
                return true;
            }
        else if (itemId == R.id.set_sort_attach) {
                changeSort(SortType.SORT_ATTACHMENT);
                return true;
            }
        else if (itemId == R.id.select_all) {
                selectAll();
                return true;
            }
        /* DIMA: Change for using in library
        switch (itemId) {
        case R.id.set_sort_date: {
            changeSort(SortType.SORT_DATE);
            return true;
        }
        case R.id.set_sort_arrival: {
            changeSort(SortType.SORT_ARRIVAL);
            return true;
        }
        case R.id.set_sort_subject: {
            changeSort(SortType.SORT_SUBJECT);
            return true;
        }
        case R.id.set_sort_sender: {
            changeSort(SortType.SORT_SENDER);
            return true;
        }
        case R.id.set_sort_flag: {
            changeSort(SortType.SORT_FLAGGED);
            return true;
        }
        case R.id.set_sort_unread: {
            changeSort(SortType.SORT_UNREAD);
            return true;
        }
        case R.id.set_sort_attach: {
            changeSort(SortType.SORT_ATTACHMENT);
            return true;
        }
        case R.id.select_all: {
            selectAll();
            return true;
        }
        }*/

        /*if (!mSingleAccountMode) {
            // None of the options after this point are "safe" for search results
            //TODO: This is not true for "unread" and "starred" searches in regular folders
            return false;
        }*/

        if (itemId == R.id.send_messages) {
                onSendPendingMessages();
                return true;
            }
        else if (itemId == R.id.expunge) {
                //if (mCurrentFolder != null) {
                    onExpunge(null, null/*mAccount, mCurrentFolder.name*/);
                //}
                return true;
            }
        else {
                return super.onOptionsItemSelected(item);
        }
        /* DIMA: Change for using in library
        switch (itemId) {
        case R.id.send_messages: {
            onSendPendingMessages();
            return true;
        }
        case R.id.expunge: {
            if (mCurrentFolder != null) {
                onExpunge(mAccount, mCurrentFolder.name);
            }
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }*/
    }

    public void onSendPendingMessages() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       // mController.sendPendingMessages(mAccount, null);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mContextMenuUniqueId == 0) {
            return false;
        }

        int adapterPosition = getPositionForUniqueId(mContextMenuUniqueId);
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return false;
        }

        if (item.getItemId() == R.id.deselect) {
            toggleMessageSelectWithAdapterPosition(adapterPosition, false);
        }
        if (item.getItemId() == R.id.select) {
                toggleMessageSelectWithAdapterPosition(adapterPosition, true);
        }
        else if (item.getItemId() == R.id.reply) {
                Message message = getMessageAtPosition(adapterPosition);
                onReply(message);
        }
        else if (item.getItemId() == R.id.forward) {
                Message message = getMessageAtPosition(adapterPosition);
                onForward(message);
        }
        else if (item.getItemId() == R.id.send_again) {
                Message message = getMessageAtPosition(adapterPosition);
                onResendMessage(message);
                mSelectedCount = 0;
        }
        else if (item.getItemId() == R.id.same_sender) {
                Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
                String senderAddress = getSenderAddressFromCursor(cursor);
                if (senderAddress != null) {
                    mFragmentListener.showMoreFromSameSender(senderAddress);
                }
        }
        else if (item.getItemId() == R.id.delete) {
                Message message = getMessageAtPosition(adapterPosition);
                onDelete(message);
        }
        else if (item.getItemId() == R.id.mark_as_read) {
                setFlag(adapterPosition, Flag.SEEN, true);
        }
        else if (item.getItemId() == R.id.mark_as_unread) {
                setFlag(adapterPosition, Flag.SEEN, false);
        }
        else if (item.getItemId() == R.id.flag) {
                setFlag(adapterPosition, Flag.FLAGGED, true);
        }
        else if (item.getItemId() == R.id.unflag) {
                setFlag(adapterPosition, Flag.FLAGGED, false);
        }
        // only if the account supports this
        else if (item.getItemId() == R.id.archive) {
                Message message = getMessageAtPosition(adapterPosition);
                //onArchive(message);
        }
        else if (item.getItemId() == R.id.spam) {
                Message message = getMessageAtPosition(adapterPosition);
                onSpam(message);
        }
        else if (item.getItemId() == R.id.move) {
                Message message = getMessageAtPosition(adapterPosition);
                onMove(message);
        }
        else if (item.getItemId() == R.id.copy) {
                Message message = getMessageAtPosition(adapterPosition);
                onCopy(message);
        }

        mContextMenuUniqueId = 0;
        return true;
    }


    private static String getSenderAddressFromCursor(Cursor cursor) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   String fromList = cursor.getString(SENDER_LIST_COLUMN);
        Address[] fromAddrs = Address.unpack(fromList);
        return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;*/
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

     /*   AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mListView.getItemAtPosition(info.position);

        if (cursor == null) {
            return;
        }

        getActivity().getMenuInflater().inflate(R.menu.message_list_item_context, menu);

        mContextMenuUniqueId = cursor.getLong(mUniqueIdColumn);
        Account account = getAccountFromCursor(cursor);

        String subject = cursor.getString(SUBJECT_COLUMN);
        boolean read = (cursor.getInt(READ_COLUMN) == 1);
        boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);

        menu.setHeaderTitle(subject);

        if(  mSelected.contains(mContextMenuUniqueId)) {
            menu.findItem(R.id.select).setVisible(false);
        } else {
            menu.findItem(R.id.deselect).setVisible(false);
        }

        if (read) {
            menu.findItem(R.id.mark_as_read).setVisible(false);
        } else {
            menu.findItem(R.id.mark_as_unread).setVisible(false);
        }

        if (flagged) {
            menu.findItem(R.id.flag).setVisible(false);
        } else {
            menu.findItem(R.id.unflag).setVisible(false);
        }

        if (!mController.isCopyCapable(account)) {
            menu.findItem(R.id.copy).setVisible(false);
        }

        if (!mController.isMoveCapable(account)) {
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
        }

        if (!account.hasArchiveFolder()) {
            menu.findItem(R.id.archive).setVisible(false);
        }

        if (!account.hasSpamFolder()) {
            menu.findItem(R.id.spam).setVisible(false);
        }
*/
    }

    public void onSwipeRightToLeft(final MotionEvent e1, final MotionEvent e2) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        // Handle right-to-left as an un-select
      //  handleSwipe(e1, false);
    }

    public void onSwipeLeftToRight(final MotionEvent e1, final MotionEvent e2) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        // Handle left-to-right as a select.
      //  handleSwipe(e1, true);
    }

    /**
     * Handle a select or unselect swipe event.
     *
     * @param downMotion
     *         Event that started the swipe
     * @param selected
     *         {@code true} if this was an attempt to select (i.e. left to right).
     */
    private void handleSwipe(final MotionEvent downMotion, final boolean selected) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* int x = (int) downMotion.getRawX();
        int y = (int) downMotion.getRawY();

        Rect headerRect = new Rect();
        mListView.getGlobalVisibleRect(headerRect);

        // Only handle swipes in the visible area of the message list
        if (headerRect.contains(x, y)) {
            int[] listPosition = new int[2];
            mListView.getLocationOnScreen(listPosition);

            int listX = x - listPosition[0];
            int listY = y - listPosition[1];

            int listViewPosition = mListView.pointToPosition(listX, listY);

            toggleMessageSelect(listViewPosition);
        }*/
    }

    private int listViewToAdapterPosition(int position) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (position > 0 && position <= mAdapter.getCount()) {
            return position - 1;
        }

        return AdapterView.INVALID_POSITION;
    }

    private int adapterToListViewPosition(int position) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (position >= 0 && position < mAdapter.getCount()) {
            return position + 1;
        }

        return AdapterView.INVALID_POSITION;
    }

    class MessageListActivityListener extends ActivityListener {
        @Override
        public void remoteSearchFailed(Account acct, String folder, final String err) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /*     mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.remote_search_error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });*/
        }

        @Override
        public void remoteSearchStarted(Account acct, String folder) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
          /*  mHandler.progress(true);
            mHandler.updateFooter(mContext.getString(R.string.remote_search_sending_query));*/
        }

        @Override
        public void enableProgressIndicator(boolean enable) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
         //   mHandler.progress(enable);
        }

        @Override
        public void remoteSearchFinished(Account acct, String folder, int numResults, List<Message> extraResults) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
          /*  mHandler.progress(false);
            mHandler.remoteSearchFinished();
            mExtraSearchResults = extraResults;
            if (extraResults != null && extraResults.size() > 0) {
                mHandler.updateFooter(String.format(mContext.getString(R.string.load_more_messages_fmt), acct.getRemoteSearchNumResults()));
            } else {
                mHandler.updateFooter("");
            }
            mFragmentListener.setMessageListProgress(Window.PROGRESS_END);*/

        }

        @Override
        public void remoteSearchServerQueryComplete(Account account, String folderName, int numResults) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
         /*   mHandler.progress(true);
            if (account != null &&  account.getRemoteSearchNumResults() != 0 && numResults > account.getRemoteSearchNumResults()) {
                mHandler.updateFooter(mContext.getString(R.string.remote_search_downloading_limited,
                        account.getRemoteSearchNumResults(), numResults));
            } else {
                mHandler.updateFooter(mContext.getString(R.string.remote_search_downloading, numResults));
            }
            mFragmentListener.setMessageListProgress(Window.PROGRESS_START);*/
        }

        @Override
        public void informUserOfStatus() {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
          //  mHandler.refreshTitle();
        }

        @Override
        public void synchronizeMailboxStarted(Account account, String folder) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
         /*   if (updateForMe(account, folder)) {
                mHandler.progress(true);
                mHandler.folderLoading(folder, true);
            }
            super.synchronizeMailboxStarted(account, folder);*/
        }

        @Override
        public void synchronizeMailboxFinished(Account account, String folder,
        int totalMessagesInMailbox, int numNewMessages) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
/*
            if (updateForMe(account, folder)) {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }
            super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);*/
        }

        @Override
        public void synchronizeMailboxFailed(Account account, String folder, String message) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
/*
            if (updateForMe(account, folder)) {
                mHandler.progress(false);
                mHandler.folderLoading(folder, false);
            }
            super.synchronizeMailboxFailed(account, folder, message);*/
        }

        @Override
        public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
         /*   if (isSingleAccountMode() && isSingleFolderMode() && mAccount.equals(account) &&
                    mFolderName.equals(folder)) {
                mUnreadMessageCount = unreadMessageCount;
            }
            super.folderStatusChanged(account, folder, unreadMessageCount);*/
        }

        private boolean updateForMe(Account account, String folder) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
         /*   if (account == null || folder == null) {
                return false;
            }

            if (!Utility.arrayContains(mAccountUuids, account.getUuid())) {
                return false;
            }

            List<String> folderNames = mSearch.getFolderNames();
            return (folderNames.size() == 0 || folderNames.contains(folder));*/
            return false;
        }
    }


    class MessageListAdapter extends CursorAdapter {

        private Drawable mAttachmentIcon;
        private Drawable mForwardedIcon;
        private Drawable mAnsweredIcon;
        private Drawable mForwardedAnsweredIcon;

        MessageListAdapter() {
            super(getActivity(), null, 0);
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            mAttachmentIcon = getResources().getDrawable(R.drawable.ic_email_attachment_small);
            mAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_answered_small);
            mForwardedIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_small);
            mForwardedAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
        }

        private String recipientSigil(boolean toMe, boolean ccMe) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if (toMe) {
                return getString(R.string.messagelist_sent_to_me_sigil);
            } else if (ccMe) {
                return getString(R.string.messagelist_sent_cc_me_sigil);
            } else {
                return "";
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            View view = mInflater.inflate(R.layout.message_list_item, parent, false);
            view.setId(R.layout.message_list_item);

            MessageViewHolder holder = new MessageViewHolder();
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.chip = view.findViewById(R.id.chip);


            if (mPreviewLines == 0 && mContactsPictureLoader == null) {
                //view.findViewById(R.id.preview).setVisibility(View.GONE);
                holder.preview = (TextView) view.findViewById(R.id.sender_compact);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged_center_right);
                //view.findViewById(R.id.flagged_bottom_right).setVisibility(View.GONE);



            } else {
                //view.findViewById(R.id.sender_compact).setVisibility(View.GONE);
                holder.preview = (TextView) view.findViewById(R.id.preview);
                holder.flagged = (CheckBox) view.findViewById(R.id.flagged_bottom_right);
                //view.findViewById(R.id.flagged_center_right).setVisibility(View.GONE);

            }

            QuickContactBadge contactBadge =
                    (QuickContactBadge) view.findViewById(R.id.contact_badge);
            if (mContactsPictureLoader != null) {
                holder.contactBadge = contactBadge;
            } else {
               // contactBadge.setVisibility(View.GONE);
            }

            if (mSenderAboveSubject) {
                holder.from = (TextView) view.findViewById(R.id.subject);
                mFontSizes.setViewTextSize(holder.from, mFontSizes.getMessageListSender());

            } else {
                holder.subject = (TextView) view.findViewById(R.id.subject);
                mFontSizes.setViewTextSize(holder.subject, mFontSizes.getMessageListSubject());

            }

            mFontSizes.setViewTextSize(holder.date, mFontSizes.getMessageListDate());


            // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
            holder.preview.setLines(Math.max(mPreviewLines,1));
            mFontSizes.setViewTextSize(holder.preview, mFontSizes.getMessageListPreview());
            // view.findViewById(R.id.selected_checkbox_wrapper).setVisibility((mCheckboxes) ? View.VISIBLE : View.GONE);

            //holder.flagged.setVisibility(mStars ? View.VISIBLE : View.GONE);
            holder.flagged.setOnClickListener(holder);


            holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
            holder.selected.setOnClickListener(holder);
            holder.selected.setVisibility(View.GONE);


            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if(true) {
                Log.e("MessageListFragment", "bindView: cursor id " + cursor.getString(0));
            }

/*            view.setBackgroundColor(Color.WHITE);
            MessageViewHolder holder = (MessageViewHolder) view.getTag();
            if(holder == null) {
                Log.e("MessageListFragment", "bindView: holder is null");
                return;
            }

            holder.chip.setBackgroundColor(Color.RED);
            holder.position = cursor.getPosition();
            holder.contactBadge.assignContactUri(null);
            holder.contactBadge.setImageResource(R.drawable.ic_contact_picture);
            holder.threadCount.setVisibility(View.GONE);
            CharSequence beforePreviewText = (mSenderAboveSubject) ? "subject" : "displayName";
            String sigil = recipientSigil(true, false);
            SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil)
                    .append(beforePreviewText);
            if (mPreviewLines > 0) {
                String preview = cursor.getString(PREVIEW_COLUMN);
                if (preview != null) {
                    messageStringBuilder.append(" ").append(preview);
                }
            }
            holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

            Drawable statusHolder = null;
            //if (forwarded && answered) {
            statusHolder = mForwardedAnsweredIcon;
            } else if (answered) {
                statusHolder = mAnsweredIcon;
            } else if (forwarded) {
                statusHolder = mForwardedIcon;
            }

            holder.from.setText("from");
            if (holder.from != null ) {
                holder.from.setTypeface(null, Typeface.BOLD);
                if (mSenderAboveSubject) {
                    holder.from.setCompoundDrawablesWithIntrinsicBounds(
                            statusHolder, // left
                            null, // top
                            null, // right
                            null); // bottom

                    holder.from.setText("displayName");
                } else {
                    holder.from.setText(new SpannableStringBuilder(sigil).append("displayName"));
                }
            }
            holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

            Spannable str = (Spannable)holder.preview.getText();

            // Create a span section for the sender, and assign the correct font size and weight
            int fontSize = (mSenderAboveSubject) ?
                    mFontSizes.getMessageListSubject():
                    mFontSizes.getMessageListSender();

            AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
            str.setSpan(span, 0, beforePreviewText.length() + sigil.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);




*/



/*








            Account account = getAccountFromCursor(cursor);

            String fromList = cursor.getString(SENDER_LIST_COLUMN);
            String toList = cursor.getString(TO_LIST_COLUMN);
            String ccList = cursor.getString(CC_LIST_COLUMN);
            Address[] fromAddrs = Address.unpack(fromList);
            Address[] toAddrs = Address.unpack(toList);
            Address[] ccAddrs = Address.unpack(ccList);

            boolean fromMe = mMessageHelper.toMe(account, fromAddrs);
            boolean toMe = mMessageHelper.toMe(account, toAddrs);
            boolean ccMe = mMessageHelper.toMe(account, ccAddrs);

            CharSequence displayName = mMessageHelper.getDisplayName(account, fromAddrs, toAddrs);
            CharSequence displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(DATE_COLUMN));

            Address counterpartyAddress = null;
            if (fromMe) {
                if (toAddrs.length > 0) {
                    counterpartyAddress = toAddrs[0];
                } else if (ccAddrs.length > 0) {
                    counterpartyAddress = ccAddrs[0];
                }
            } else if (fromAddrs.length > 0) {
                counterpartyAddress = fromAddrs[0];
            }

            int threadCount = (mThreadedList) ? cursor.getInt(THREAD_COUNT_COLUMN) : 0;

            String subject = cursor.getString(SUBJECT_COLUMN);
            if (StringUtils.isNullOrEmpty(subject)) {
                subject = getString(R.string.general_no_subject);
            } else if (threadCount > 1) {
                // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
                subject = Utility.stripSubject(subject);
            }

            //DIMA TODO: add dividing read and unread messages
            boolean read = (cursor.getInt(READ_COLUMN) == 1);
            boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);
            boolean answered = (cursor.getInt(ANSWERED_COLUMN) == 1);
            boolean forwarded = (cursor.getInt(FORWARDED_COLUMN) == 1);

            boolean hasAttachments = (cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0);
*/
            MessageViewHolder holder = (MessageViewHolder) view.getTag();
/*
            int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;

            long uniqueId = cursor.getLong(mUniqueIdColumn);
            boolean selected = mSelected.contains(uniqueId);


            holder.chip.setBackgroundColor(account.getChipColor());
/*
            if (mCheckboxes) {
                holder.selected.setChecked(selected);
            }
*/
/*            if (mStars) {
                holder.flagged.setChecked(flagged);
            }*/
            Log.e("", "possition is: " + cursor.getPosition());
            holder.position = cursor.getPosition();
/*
            if (holder.contactBadge != null) {
                if (counterpartyAddress != null) {
                //DIMA TODO: add getting contact avatar
                    holder.contactBadge.assignContactFromEmail(counterpartyAddress.getAddress(), true);*/
                    /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but QuickContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */
/*                    holder.contactBadge.setPadding(0, 0, 0, 0);
                    mContactsPictureLoader.loadContactPicture(counterpartyAddress, holder.contactBadge);
                } else {
                    holder.contactBadge.assignContactUri(null);
                    holder.contactBadge.setImageResource(R.drawable.ic_contact_picture);
                }
            }
*/

            //DIMA TODO: add coloring items by selected\read\common
            // Background color
            int res;
            if(null != mActiveMsgId && cursor.getPosition() +1 == mActiveMsgId) {
            res = R.attr.messageListSelectedBackgroundColor;
            } else {
                res = R.attr.messageListUnreadItemBackgroundColor;
            }

            TypedValue outValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(res, outValue, true);
            view.setBackgroundColor(outValue.data);

/*
            if (mActiveMessage != null) {
                String uid = cursor.getString(UID_COLUMN);
                String folderName = cursor.getString(FOLDER_NAME_COLUMN);

                if (account.getUuid().equals(mActiveMessage.accountUuid) &&
                        folderName.equals(mActiveMessage.folderName) &&
                        uid.equals(mActiveMessage.uid)) {
                    int res = R.attr.messageListActiveItemBackgroundColor;

                    TypedValue outValue = new TypedValue();
                    getActivity().getTheme().resolveAttribute(res, outValue, true);
                    view.setBackgroundColor(outValue.data);
                }
            }


            CharSequence beforePreviewText = (mSenderAboveSubject) ? subject : displayName;

            String sigil = recipientSigil(toMe, ccMe);

            SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil)
                    .append(beforePreviewText);

            if (mPreviewLines > 0) {
                String preview = cursor.getString(PREVIEW_COLUMN);
                if (preview != null) {
                    messageStringBuilder.append(" ").append(preview);
                }
            }

    //DIMA TODO: add setting text style setting
            holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

            Spannable str = (Spannable)holder.preview.getText();

            // Create a span section for the sender, and assign the correct font size and weight
            int fontSize = (mSenderAboveSubject) ?
                    mFontSizes.getMessageListSubject():
                    mFontSizes.getMessageListSender();

            AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
            str.setSpan(span, 0, beforePreviewText.length() + sigil.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            //TODO: make this part of the theme
            int color = (K9.getK9Theme() == K9.Theme.LIGHT) ?
                    Color.rgb(105, 105, 105) :
                    Color.rgb(160, 160, 160);

            // Set span (color) for preview message
            str.setSpan(new ForegroundColorSpan(color), beforePreviewText.length() + sigil.length(),
                    str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            Drawable statusHolder = null;
            if (forwarded && answered) {
                statusHolder = mForwardedAnsweredIcon;
            } else if (answered) {
                statusHolder = mAnsweredIcon;
            } else if (forwarded) {
                statusHolder = mForwardedIcon;
            }

            if (holder.from != null ) {
                holder.from.setTypeface(null, maybeBoldTypeface);
                if (mSenderAboveSubject) {
                    holder.from.setCompoundDrawablesWithIntrinsicBounds(
                            statusHolder, // left
                            null, // top
                            hasAttachments ? mAttachmentIcon : null, // right
                            null); // bottom

                    holder.from.setText(displayName);
                } else {
                    holder.from.setText(new SpannableStringBuilder(sigil).append(displayName));
                }
            }

            if (holder.subject != null ) {
                if (!mSenderAboveSubject) {
                    holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                            statusHolder, // left
                            null, // top
                            hasAttachments ? mAttachmentIcon : null, // right
                            null); // bottom
                }

                holder.subject.setTypeface(null, maybeBoldTypeface);
                holder.subject.setText(subject);
            }

            holder.date.setText(displayDate);*/
        }
    }

    class MessageViewHolder implements View.OnClickListener {
        public TextView subject;
        public TextView preview;
        public TextView from;
        public TextView time;
        public TextView date;
        public View chip;
        public TextView threadCount;
        public CheckBox flagged;
        public CheckBox selected;
        public int position = -1;
        public QuickContactBadge contactBadge;
        @Override
        public void onClick(View view) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
            + ": view " + view.getClass().getName() + "; possition " + position);
            if (position != -1) {

               if (view.getId() == R.id.selected_checkbox) {
                   CheckBox checkBox = (CheckBox)view;

                   toggleMessageSelectWithAdapterPosition(position, checkBox.isChecked());
               }
               else if (view.getId() == R.id.flagged_bottom_right ||
                        view.getId() == R.id.flagged_center_right) {
                   Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                   + ": flagged button " + (view.getId() == R.id.flagged_bottom_right ? "bottom" : "center"));
                   CheckBox checkBox = (CheckBox) view;
                   toggleMessageFlagWithAdapterPosition(position, checkBox.isChecked());
               }
               else {
                   Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                           + ": view Id hasn't been matched " + view.getId());
               }

                /* DIMA: Change for using in library
                switch (view.getId()) {
                    case R.id.selected_checkbox:
                        toggleMessageSelectWithAdapterPosition(position);
                        break;
                    case R.id.flagged_bottom_right:
                    case R.id.flagged_center_right:
                        toggleMessageFlagWithAdapterPosition(position);
                        break;
                }*/
            }
        }
    }


    private View getFooterView(ViewGroup parent) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mFooterView == null) {
            mFooterView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
            mFooterView.setId(R.layout.message_list_item_footer);
            FooterViewHolder holder = new FooterViewHolder();
            holder.main = (TextView) mFooterView.findViewById(R.id.main_text);
            mFooterView.setTag(holder);
        }

        return mFooterView;
    }

    private void updateFooterView() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /*if (!mSearch.isManualSearch() && mCurrentFolder != null && mAccount != null) {
            if (mCurrentFolder.loading) {
                updateFooter(mContext.getString(R.string.status_loading_more));
            } else {
                String message;
                if (!mCurrentFolder.lastCheckFailed) {
                    if (mAccount.getDisplayCount() == 0) {
                        message = mContext.getString(R.string.message_list_load_more_messages_action);
                    } else {
                        message = String.format(mContext.getString(R.string.load_more_messages_fmt), mAccount.getDisplayCount());
                    }
                } else {
                    message = mContext.getString(R.string.status_loading_more_failed);
                }
                updateFooter(message);
            }
        } else {*/
            updateFooter(null);
        //}
    }

    public void updateFooter(final String text) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mFooterView == null) {
            return;
        }

        FooterViewHolder holder = (FooterViewHolder) mFooterView.getTag();

        if (text != null) {
            holder.main.setText(text);
        }
        if (holder.main.getText().length() > 0) {
            holder.main.setVisibility(View.VISIBLE);
        } else {
           // holder.main.setVisibility(View.GONE);
        }
    }

    static class FooterViewHolder {
        public TextView main;
    }

    /**
     * Set selection state for all messages.
     *
     * @param selected
     *         If {@code true} all messages get selected. Otherwise, all messages get deselected and
     *         action mode is finished.
     */
    private void setSelectionState(boolean selected) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (selected) {
            if (mAdapter.getCount() == 0) {
                // Nothing to do if there are no messages
                return;
            }

            mSelectedCount = 0;
            for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
                Cursor cursor = (Cursor) mAdapter.getItem(i);
                long uniqueId = cursor.getLong(mUniqueIdColumn);
                mSelected.add(uniqueId);

                if (mThreadedList) {
                    int threadCount = cursor.getInt(THREAD_COUNT_COLUMN);
                    mSelectedCount += (threadCount > 1) ? threadCount : 1;
                } else {
                    mSelectedCount++;
                }
            }

            if (mActionMode == null) {
                mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
            }
            computeBatchDirection();
            updateActionModeTitle();
            computeSelectAllVisibility();
        } else {
            mSelected.clear();
            mSelectedCount = 0;
            if (mActionMode != null) {
                mActionMode.finish();
                mActionMode = null;
            }
        }

        mAdapter.notifyDataSetChanged();*/
    }

    private void toggleMessageSelect(int listViewPosition) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  int adapterPosition = listViewToAdapterPosition(listViewPosition);
        if (adapterPosition == AdapterView.INVALID_POSITION) {
            return;
        }

        toggleMessageSelectWithAdapterPosition(adapterPosition);*/
    }

    private void toggleMessageFlagWithAdapterPosition(int adapterPosition, boolean isChecked) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": adapterPosition " + adapterPosition + "; isChecked " + isChecked);
     /*   Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);
*/
        setFlag(adapterPosition,Flag.FLAGGED, isChecked/*!flagged*/);
    }

    private void toggleMessageSelectWithAdapterPosition(int adapterPosition, boolean isChecked) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
         + ": adapterPosition " + adapterPosition + "; isChecked " + isChecked);
    /*    Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        long uniqueId = cursor.getLong(mUniqueIdColumn);

        boolean selected = mSelected.contains(uniqueId);
        if (!selected) {
            mSelected.add(uniqueId);
        } else {
            mSelected.remove(uniqueId);
        }

        int selectedCountDelta = 1;
        if (mThreadedList) {
            int threadCount = cursor.getInt(THREAD_COUNT_COLUMN);
            if (threadCount > 1) {
                selectedCountDelta = threadCount;
            }
        }

        if (mActionMode != null) {
            if (mSelectedCount == selectedCountDelta && selected) {
                mActionMode.finish();
                mActionMode = null;
                return;
            }
        } else {
            mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
        }

        if (selected) {
            mSelectedCount -= selectedCountDelta;
        } else {
            mSelectedCount += selectedCountDelta;
        }

        computeBatchDirection();
        updateActionModeTitle();

        // make sure the onPrepareActionMode is called
        mActionMode.invalidate();

        computeSelectAllVisibility();

        mAdapter.notifyDataSetChanged();*/
    }

    private void updateActionModeTitle() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));
    }

    private void computeSelectAllVisibility() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  mActionModeCallback.showSelectAll(mSelected.size() != mAdapter.getCount());
    }

    private void computeBatchDirection() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   boolean isBatchFlag = false;
        boolean isBatchRead = false;

        for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                boolean read = (cursor.getInt(READ_COLUMN) == 1);
                boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);

                if (!flagged) {
                    isBatchFlag = true;
                }
                if (!read) {
                    isBatchRead = true;
                }

                if (isBatchFlag && isBatchRead) {
                    break;
                }
            }
        }

        mActionModeCallback.showMarkAsRead(isBatchRead);
        mActionModeCallback.showFlag(isBatchFlag);*/
    }

    private void setFlag(int adapterPosition, final Flag flag, final boolean newState) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (adapterPosition == AdapterView.INVALID_POSITION) {
            return;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        Account account = mPreferences.getAccount(cursor.getString(ACCOUNT_UUID_COLUMN));

        if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
            long threadRootId = cursor.getLong(THREAD_ROOT_COLUMN);
            mController.setFlagForThreads(account,
                    Collections.singletonList(Long.valueOf(threadRootId)), flag, newState);
        } else {
            long id = cursor.getLong(ID_COLUMN);
            mController.setFlag(account, Collections.singletonList(Long.valueOf(id)), flag,
                    newState);
        }

        computeBatchDirection();*/
    }

/*    private void setFlagForSelected(final Flag flag, final boolean newState) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    if (mSelected.size() == 0) {
        return;
    }

    Map<Account, List<Long>> messageMap = new HashMap<Account, List<Long>>();
    Map<Account, List<Long>> threadMap = new HashMap<Account, List<Long>>();
    Set<Account> accounts = new HashSet<Account>();

    for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        long uniqueId = cursor.getLong(mUniqueIdColumn);

        if (mSelected.contains(uniqueId)) {
            String uuid = cursor.getString(ACCOUNT_UUID_COLUMN);
            Account account = mPreferences.getAccount(uuid);
            accounts.add(account);

            if (mThreadedList && cursor.getInt(THREAD_COUNT_COLUMN) > 1) {
                List<Long> threadRootIdList = threadMap.get(account);
                if (threadRootIdList == null) {
                    threadRootIdList = new ArrayList<Long>();
                    threadMap.put(account, threadRootIdList);
                }

                threadRootIdList.add(cursor.getLong(THREAD_ROOT_COLUMN));
            } else {
                List<Long> messageIdList = messageMap.get(account);
                if (messageIdList == null) {
                    messageIdList = new ArrayList<Long>();
                    messageMap.put(account, messageIdList);
                }

                messageIdList.add(cursor.getLong(ID_COLUMN));
            }
        }
    }

    for (Account account : accounts) {
        List<Long> messageIds = messageMap.get(account);
        List<Long> threadRootIds = threadMap.get(account);

        if (messageIds != null) {
            mController.setFlag(account, messageIds, flag, newState);
        }

        if (threadRootIds != null) {
            mController.setFlagForThreads(account, threadRootIds, flag, newState);
        }
    }

    computeBatchDirection();
}
*/
    private void onMove(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  onMove(Collections.singletonList(message));
    }

    /**
     * Display the message move activity.
     *
     * @param messages
     *         Never {@code null}.
     */
/*    private void onMove(List<Message> messages) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    if (!checkCopyOrMovePossible(messages, FolderOperation.MOVE)) {
        return;
    }

    final Folder folder;
    if (mIsThreadDisplay) {
        folder = messages.get(0).getFolder();
    } else if (mSingleFolderMode) {
        folder = mCurrentFolder.folder;
    } else {
        folder = null;
    }

    Account account = messages.get(0).getFolder().getAccount();

    displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, account, folder, messages);
}
*/
    private void onCopy(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  onCopy(Collections.singletonList(message));
    }

    /**
     * Display the message copy activity.
     *
     * @param messages
     *         Never {@code null}.
     */
/*    private void onCopy(List<Message> messages) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    if (!checkCopyOrMovePossible(messages, FolderOperation.COPY)) {
        return;
    }

    final Folder folder;
    if (mIsThreadDisplay) {
        folder = messages.get(0).getFolder();
    } else if (mSingleFolderMode) {
        folder = mCurrentFolder.folder;
    } else {
        folder = null;
    }

    displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, mAccount, folder, messages);
}
*/
    /**
     * Helper method to manage the invocation of {@link #startActivityForResult(Intent, int)} for a
     * folder operation ({@link ChooseFolder} activity), while saving a list of associated messages.
     *
     * @param requestCode
     *         If {@code >= 0}, this code will be returned in {@code onActivityResult()} when the
     *         activity exits.
     * @param folder
     *         The source folder. Never {@code null}.
     * @param messages
     *         Messages to be affected by the folder operation. Never {@code null}.
     *
     * @see #startActivityForResult(Intent, int)
     */
/*private void displayFolderChoice(int requestCode, Account account, Folder folder,
        List<Message> messages) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

    Intent intent = new Intent(getActivity(), ChooseFolder.class);
    intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, account.getUuid());
    intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, account.getLastSelectedFolderName());

    if (folder == null) {
        intent.putExtra(ChooseFolder.EXTRA_SHOW_CURRENT, "yes");
    } else {
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
    }

    // remember the selected messages for #onActivityResult
    mActiveMessages = messages;
    startActivityForResult(intent, requestCode);
}

private void onArchive(final Message message) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
 //   onArchive(Collections.singletonList(message));
}

private void onArchive(final List<Message> messages) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    Map<Account, List<Message>> messagesByAccount = groupMessagesByAccount(messages);

    for (Entry<Account, List<Message>> entry : messagesByAccount.entrySet()) {
        Account account = entry.getKey();
        String archiveFolder = account.getArchiveFolderName();

        if (!K9.FOLDER_NONE.equals(archiveFolder)) {
            move(entry.getValue(), archiveFolder);
        }
    }
}
*/
    //DIMA TODO: check if I can use it for grouping conversations
    private Map<Account, List<Message>> groupMessagesByAccount(final List<Message> messages) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  Map<Account, List<Message>> messagesByAccount = new HashMap<Account, List<Message>>();
        for (Message message : messages) {
            Account account = message.getFolder().getAccount();

            List<Message> msgList = messagesByAccount.get(account);
            if (msgList == null) {
                msgList = new ArrayList<Message>();
                messagesByAccount.put(account, msgList);
            }

            msgList.add(message);
        }
        return messagesByAccount;*/
        return null;
    }

    private void onSpam(Message message) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  onSpam(Collections.singletonList(message));
    }

    /**
     * Move messages to the spam folder.
     *
     * @param messages
     *         The messages to move to the spam folder. Never {@code null}.
     */
/*    private void onSpam(List<Message> messages) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (K9.confirmSpam()) {
            // remember the message selection for #onCreateDialog(int)
            mActiveMessages = messages;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            onSpamConfirmed(messages);
        }
    }
*/
/*
    private void onSpamConfirmed(List<Message> messages) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        Map<Account, List<Message>> messagesByAccount = groupMessagesByAccount(messages);

        for (Entry<Account, List<Message>> entry : messagesByAccount.entrySet()) {
            Account account = entry.getKey();
            String spamFolder = account.getSpamFolderName();

            if (!K9.FOLDER_NONE.equals(spamFolder)) {
                move(entry.getValue(), spamFolder);
            }
        }
    }

    private static enum FolderOperation {
        COPY, MOVE
    }
*/
    /**
     * Display a Toast message if any message isn't synchronized
     *
     * @param messages
     *         The messages to copy or move. Never {@code null}.
     * @param operation
     *         The type of operation to perform. Never {@code null}.
     *
     * @return {@code true}, if operation is possible.
     */
/*    private boolean checkCopyOrMovePossible(final List<Message> messages,
            final FolderOperation operation) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        if (messages.size() == 0) {
            return false;
        }

        boolean first = true;
        for (final Message message : messages) {
            if (first) {
                first = false;
                // account check
                final Account account = message.getFolder().getAccount();
                if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) ||
                        (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                    return false;
                }
            }
            // message check
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                    (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                                                   Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        }
        return true;
    }
*/
    /**
     * Copy the specified messages to the specified folder.
     *
     * @param messages
     *         List of messages to copy. Never {@code null}.
     * @param destination
     *         The name of the destination folder. Never {@code null}.
     */
/*    private void copy(List<Message> messages, final String destination) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        copyOrMove(messages, destination, FolderOperation.COPY);
    }
*/
    /**
     * Move the specified messages to the specified folder.
     *
     * @param messages
     *         The list of messages to move. Never {@code null}.
     * @param destination
     *         The name of the destination folder. Never {@code null}.
     */
/*    private void move(List<Message> messages, final String destination) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        copyOrMove(messages, destination, FolderOperation.MOVE);
    }
*/
    /**
     * The underlying implementation for {@link #copy(List, String)} and
     * {@link #move(List, String)}. This method was added mainly because those 2
     * methods share common behavior.
     *
     * @param messages
     *         The list of messages to copy or move. Never {@code null}.
     * @param destination
     *         The name of the destination folder. Never {@code null} or {@link K9#FOLDER_NONE}.
     * @param operation
     *         Specifies what operation to perform. Never {@code null}.
     */
/*    private void copyOrMove(List<Message> messages, final String destination,
            final FolderOperation operation) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());

        Map<String, List<Message>> folderMap = new HashMap<String, List<Message>>();

        for (Message message : messages) {
            if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                    (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {

                Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                        Toast.LENGTH_LONG).show();

                // XXX return meaningful error value?

                // message isn't synchronized
                return;
            }

            String folderName = message.getFolder().getName();
            if (folderName.equals(destination)) {
                // Skip messages already in the destination folder
                continue;
            }

            List<Message> outMessages = folderMap.get(folderName);
            if (outMessages == null) {
                outMessages = new ArrayList<Message>();
                folderMap.put(folderName, outMessages);
            }

            outMessages.add(message);
        }

        for (Map.Entry<String, List<Message>> entry : folderMap.entrySet()) {
            String folderName = entry.getKey();
            List<Message> outMessages = entry.getValue();
            Account account = outMessages.get(0).getFolder().getAccount();

            if (operation == FolderOperation.MOVE) {
                if (mThreadedList) {
                    mController.moveMessagesInThread(account, folderName, outMessages, destination);
                } else {
                    mController.moveMessages(account, folderName, outMessages, destination, null);
                }
            } else {
                if (mThreadedList) {
                    mController.copyMessagesInThread(account, folderName, outMessages, destination);
                } else {
                    mController.copyMessages(account, folderName, outMessages, destination, null);
                }
            }
        }
    }
*/

/*
    class ActionModeCallback implements ActionMode.Callback {
        private MenuItem mSelectAll;
        private MenuItem mMarkAsRead;
        private MenuItem mMarkAsUnread;
        private MenuItem mFlag;
        private MenuItem mUnflag;

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            mSelectAll = menu.findItem(R.id.select_all);
            mMarkAsRead = menu.findItem(R.id.mark_as_read);
            mMarkAsUnread = menu.findItem(R.id.mark_as_unread);
            mFlag = menu.findItem(R.id.flag);
            mUnflag = menu.findItem(R.id.unflag);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            mActionMode = null;
            mSelectAll = null;
            mMarkAsRead = null;
            mMarkAsUnread = null;
            mFlag = null;
            mUnflag = null;
            setSelectionState(false);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.message_list_context, menu);

            // check capabilities
            setContextCapabilities(null, menu);

            return true;
        }
*/
        /**
         * Disables menu options not supported by the account type or current "search view".
         *
         * @param account
         *         The account to query for its capabilities.
         * @param menu
         *         The menu to adapt.
         */
       /* private void setContextCapabilities(Account account, Menu menu) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if (!mSingleAccountMode) {
                // We don't support cross-account copy/move operations right now
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.copy).setVisible(false);

                //TODO: we could support the archive and spam operations if all selected messages
                // belong to non-POP3 accounts
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);

            } else {
                // hide unsupported
                if (!mController.isCopyCapable(account)) {
                    menu.findItem(R.id.copy).setVisible(false);
                }

                if (!mController.isMoveCapable(account)) {
                    menu.findItem(R.id.move).setVisible(false);
                    menu.findItem(R.id.archive).setVisible(false);
                    menu.findItem(R.id.spam).setVisible(false);
                }

                if (!account.hasArchiveFolder()) {
                    menu.findItem(R.id.archive).setVisible(false);
                }

                if (!account.hasSpamFolder()) {
                    menu.findItem(R.id.spam).setVisible(false);
                }
            }
        }

        public void showSelectAll(boolean show) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if (mActionMode != null) {
                mSelectAll.setVisible(show);
            }
        }

        public void showMarkAsRead(boolean show) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if (mActionMode != null) {
                mMarkAsRead.setVisible(show);
                mMarkAsUnread.setVisible(!show);
            }
        }

        public void showFlag(boolean show) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
            if (mActionMode != null) {
                mFlag.setVisible(show);
                mUnflag.setVisible(!show);
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
           */ /*
             * In the following we assume that we can't move or copy
             * mails to the same folder. Also that spam isn't available if we are
             * in the spam folder,same for archive.
             *
             * This is the case currently so safe assumption.
             *//*

            if (item.getItemId() == R.id.delete) {
                List<Message> messages = getCheckedMessages();
                onDelete(messages);
                mSelectedCount = 0;
            }
            else if (item.getItemId() == R.id.mark_as_read) {
                    setFlagForSelected(Flag.SEEN, true);
            }
            else if (item.getItemId() == R.id.mark_as_unread) {
                    setFlagForSelected(Flag.SEEN, false);
            }
            else if (item.getItemId() == R.id.flag) {
                    setFlagForSelected(Flag.FLAGGED, true);
            }
            else if (item.getItemId() == R.id.unflag) {
                    setFlagForSelected(Flag.FLAGGED, false);
            }
            else if (item.getItemId() == R.id.select_all) {
                    selectAll();
            }
            // only if the account supports this
            else if (item.getItemId() == R.id.archive) {
                    List<Message> messages = getCheckedMessages();
                    onArchive(messages);
                    mSelectedCount = 0;
            }
            else if (item.getItemId() == R.id.spam) {
                    List<Message> messages = getCheckedMessages();
                    onSpam(messages);
                    mSelectedCount = 0;
            }
            else if (item.getItemId() == R.id.move) {
                    List<Message> messages = getCheckedMessages();
                    onMove(messages);
                    mSelectedCount = 0;
            }
            else if (item.getItemId() == R.id.copy) {
                    List<Message> messages = getCheckedMessages();
                    onCopy(messages);
                    mSelectedCount = 0;
            }
            if (mSelectedCount == 0) {
                mActionMode.finish();
            }

            return true;
        }
    }*/

    @Override
    public void doPositiveClick(int dialogId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (dialogId == R.id.dialog_confirm_spam) {
               /* onSpamConfirmed(mActiveMessages);
                // No further need for this reference
                mActiveMessages = null;*/
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (dialogId == R.id.dialog_confirm_spam) {
                // No further need for this reference
                mActiveMsgId = null;
        }
        /* DIMA: Change for using in library
        switch (dialogId) {
            case R.id.dialog_confirm_spam:
            case R.id.dialog_confirm_delete: {
                // No further need for this reference
                mActiveMessages = null;
                break;
            }
        }*/
    }

    @Override
    public void dialogCancelled(int dialogId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        doNegativeClick(dialogId);
    }

    public void checkMail() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   if (isSingleAccountMode() && isSingleFolderMode()) {
            mController.synchronizeMailbox(mAccount, mFolderName, mListener, null);
            mController.sendPendingMessages(mAccount, mListener);
        } else if (mAllAccounts) {
            mController.checkMail(mContext, null, true, true, mListener);
        } else {
            for (String accountUuid : mAccountUuids) {
                Account account = mPreferences.getAccount(accountUuid);
                mController.checkMail(mContext, account, true, true, mListener);
            }
        }*/
    }

    /**
     * We need to do some special clean up when leaving a remote search result screen. If no
     * remote search is in progress, this method does nothing special.
     */
    @Override
    public void onStop() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   // If we represent a remote search, then kill that before going back.
        if (isRemoteSearch() && mRemoteSearchFuture != null) {
            try {
                Log.i(K9.LOG_TAG, "Remote search in progress, attempting to abort...");
                // Canceling the future stops any message fetches in progress.
                final boolean cancelSuccess = mRemoteSearchFuture.cancel(true);   // mayInterruptIfRunning = true
                if (!cancelSuccess) {
                    Log.e(K9.LOG_TAG, "Could not cancel remote search future.");
                }
                // Closing the folder will kill off the connection if we're mid-search.
                final Account searchAccount = mAccount;
                final Folder remoteFolder = mCurrentFolder.folder;
                remoteFolder.close();
                // Send a remoteSearchFinished() message for good measure.
                mListener.remoteSearchFinished(searchAccount, mCurrentFolder.name, 0, null);
            } catch (Exception e) {
                // Since the user is going back, log and squash any exceptions.
                Log.e(K9.LOG_TAG, "Could not abort remote search before going back", e);
            }
        }*/
        super.onStop();
    }

    public ArrayList<MessageReference> getMessageReferences() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        ArrayList<MessageReference> messageRefs = new ArrayList<MessageReference>();
/*
        for (int i = 0, len = mAdapter.getCount(); i < len; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);

            MessageReference ref = new MessageReference();
            ref.accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
            ref.folderName = cursor.getString(FOLDER_NAME_COLUMN);
            ref.uid = cursor.getString(UID_COLUMN);

            messageRefs.add(ref);
        }

        return messageRefs;*/
        return null;
    }

    public void selectAll() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  setSelectionState(true);
    }

    public void onMoveUp() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  int currentPosition = mListView.getSelectedItemPosition();
        if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
            currentPosition = mListView.getFirstVisiblePosition();
        }
        if (currentPosition > 0) {
            mListView.setSelection(currentPosition - 1);
        }*/
    }

    public void onMoveDown() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   int currentPosition = mListView.getSelectedItemPosition();
        if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
            currentPosition = mListView.getFirstVisiblePosition();
        }

        if (currentPosition < mListView.getCount()) {
            mListView.setSelection(currentPosition + 1);
        }*/
    }

    public boolean openPrevious(MessageReference messageReference) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  int position = getPosition(messageReference);
        if (position <= 0) {
            return false;
        }

        openMessageAtPosition(position - 1);*/
        return true;
    }

    public boolean openNext(MessageReference messageReference) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   int position = getPosition(messageReference);
        if (position < 0 || position == mAdapter.getCount() - 1) {
            return false;
        }

        openMessageAtPosition(position + 1);*/
        return true;
    }

    public boolean isFirst(Integer msgId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return mAdapter.isEmpty() || (msgId == 0);
    }

    public boolean isLast(Integer msgId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return mAdapter.isEmpty() || (msgId == mAdapter.getCount() - 1);
    }

    private MessageReference getReferenceForPosition(int position) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   Cursor cursor = (Cursor) mAdapter.getItem(position);
        MessageReference ref = new MessageReference();
        ref.accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
        ref.folderName = cursor.getString(FOLDER_NAME_COLUMN);
        ref.uid = cursor.getString(UID_COLUMN);

        return ref;*/
        return null;
    }

    private void openMessageAtPosition(int position) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        // Scroll message into view if necessary
      /*  int listViewPosition = adapterToListViewPosition(position);
        if (listViewPosition != AdapterView.INVALID_POSITION &&
                (listViewPosition < mListView.getFirstVisiblePosition() ||
                listViewPosition > mListView.getLastVisiblePosition())) {
            mListView.setSelection(listViewPosition);
        }

        MessageReference ref = getReferenceForPosition(position);

        // For some reason the mListView.setSelection() above won't do anything when we call
        // onOpenMessage() (and consequently mAdapter.notifyDataSetChanged()) right away. So we
        // defer the call using MessageListHandler.
        mHandler.openMessage(ref);*/
    }

    private int getPosition(MessageReference messageReference) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  for (int i = 0, len = mAdapter.getCount(); i < len; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);

            String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
            String folderName = cursor.getString(FOLDER_NAME_COLUMN);
            String uid = cursor.getString(UID_COLUMN);

            if (accountUuid.equals(messageReference.accountUuid) &&
                    folderName.equals(messageReference.folderName) &&
                    uid.equals(messageReference.uid)) {
                return i;
            }
        }
*/
        return -1;
    }

    public interface MessageListFragmentListener {
        void setMessageListProgress(int level);
        void showThread(Account account, String folderName, long rootId);
        void showMoreFromSameSender(String senderAddress);
        void onResendMessage(Message message);
        void onForward(Message message);
        void onReply(Message message);
        void onReplyAll(Message message);
        void openMessage(Integer id/*MessageReference messageReference*/);
        void setMessageListTitle(String title);
        void setMessageListSubTitle(String subTitle);
        void setUnreadCount(int unread);
        void onCompose();
        boolean startSearch(Account account, String folderName);
        void remoteSearchStarted();
        void goBack();
        void updateMenu();
    }

    public void onReverseSort() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  changeSort(mSortType);
    }

    private Message getSelectedMessage() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   int listViewPosition = mListView.getSelectedItemPosition();
        int adapterPosition = listViewToAdapterPosition(listViewPosition);

        return getMessageAtPosition(adapterPosition);*/
        return null;
    }

    private int getAdapterPositionForSelectedMessage() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  int listViewPosition = mListView.getSelectedItemPosition();
        return listViewToAdapterPosition(listViewPosition);*/
        return -1;
    }

    private int getPositionForUniqueId(long uniqueId) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            if (cursor.getLong(mUniqueIdColumn) == uniqueId) {
                return position;
            }
        }*/

        return AdapterView.INVALID_POSITION;
    }

    private Message getMessageAtPosition(int adapterPosition) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (adapterPosition == AdapterView.INVALID_POSITION) {
            return null;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        String uid = cursor.getString(UID_COLUMN);

        Account account = getAccountFromCursor(cursor);
        long folderId = cursor.getLong(FOLDER_ID_COLUMN);
        Folder folder = getFolderById(account, folderId);

        try {
            return folder.getMessage(uid);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Something went wrong while fetching a message", e);
        }
*/
        return null;
    }

    private List<Message> getCheckedMessages() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
  /*      List<Message> messages = new ArrayList<Message>(mSelected.size());
        for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                Message message = getMessageAtPosition(position);
                if (message != null) {
                    messages.add(message);
                }
            }
        }

        return messages;*/
        return null;
    }

    public void onDelete() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   Message message = getSelectedMessage();
        if (message != null) {
            onDelete(Collections.singletonList(message));
        }*/
    }

    public void toggleMessageSelect() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       // toggleMessageSelect(mListView.getSelectedItemPosition());
    }

    public void onToggleFlagged() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       // onToggleFlag(Flag.FLAGGED, FLAGGED_COLUMN);
    }

    public void onToggleRead() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  onToggleFlag(Flag.SEEN, READ_COLUMN);
    }

    private void onToggleFlag(Flag flag, int flagColumn) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* int adapterPosition = getAdapterPositionForSelectedMessage();
        if (adapterPosition == ListView.INVALID_POSITION) {
            return;
        }

        Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
        boolean flagState = (cursor.getInt(flagColumn) == 1);
        setFlag(adapterPosition, flag, !flagState);*/
    }

    public void onMove() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* Message message = getSelectedMessage();
        if (message != null) {
            onMove(message);
        }*/
    }

    public void onArchive() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  Message message = getSelectedMessage();
        if (message != null) {
            onArchive(message);
        }*/
    }

    public void onCopy() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  Message message = getSelectedMessage();
        if (message != null) {
            onCopy(message);
        }*/
    }

    public boolean isOutbox() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; //(mFolderName != null && mFolderName.equals(mAccount.getOutboxFolderName()));
    }

    public boolean isErrorFolder() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // K9.ERROR_FOLDER_NAME.equals(mFolderName);
    }

    public boolean isRemoteFolder() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        /*if (mSearch.isManualSearch() || isOutbox() || isErrorFolder()) {
            return false;
        }

        if (!mController.isMoveCapable(mAccount)) {
            // For POP3 accounts only the Inbox is a remote folder.
            return (mFolderName != null && mFolderName.equals(mAccount.getInboxFolderName()));
        }
*/
        return true;
    }

    public boolean isManualSearch() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // mSearch.isManualSearch();
    }

    public boolean isAccountExpungeCapable() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* try {
            return (mAccount != null && mAccount.getRemoteStore().isExpungeCapable());
        } catch (Exception e) {
            return false;
        }*/
        return false;
    }

    public boolean isRemoteSearch() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // mRemoteSearchPerformed;
    }

    public boolean isRemoteSearchAllowed() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* if (!mSearch.isManualSearch() || mRemoteSearchPerformed || !mSingleFolderMode) {
            return false;
        }

        boolean allowRemoteSearch = false;
        final Account searchAccount = mAccount;
        if (searchAccount != null) {
            allowRemoteSearch = searchAccount.allowRemoteSearch();
        }

        return allowRemoteSearch;*/
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());


        if(null == mContentObserver)
            mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                Log.e("Observer1", "onChange!!!");
                getLoaderManager().getLoader(0).forceLoad();
            }
        };

        mContext.getContentResolver().registerContentObserver(MessagerProvider.CONTENT_URI, true,
                mContentObserver);

        Uri uri = MessagerProvider.CONTENT_URI;
        String[] projection = new String[] {
                MessagerProvider.ID,
                MessagerProvider.NAME,
                MessagerProvider.BIRTHDAY
        };

        //DIMA TODO: complete sorting functionality
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": uri " + uri.toString() + "; sortOrder " + buildSortOrder());
        return new CursorLoader(getActivity(), uri, projection, null, null, null/*buildSortOrder()*/);
    }
/*
private String getThreadId(LocalSearch search) {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    for (ConditionsTreeNode node : search.getLeafSet()) {
        SearchCondition condition = node.mCondition;
        if (condition.field == Searchfield.THREAD_ID) {
            return condition.value;
        }
    }

    return null;
}
*/
    private String buildSortOrder() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": mSortType " + mSortType);
        String sortColumn = MessageColumns.ID;
        switch (mSortType) {
            case SORT_ARRIVAL: {
                sortColumn = MessageColumns.INTERNAL_DATE;
                break;
            }
            case SORT_ATTACHMENT: {
                sortColumn = "(" + MessageColumns.ATTACHMENT_COUNT + " < 1)";
                break;
            }
            case SORT_FLAGGED: {
                sortColumn = "(" + MessageColumns.FLAGGED + " != 1)";
                break;
            }
            case SORT_SENDER: {
                //FIXME
                sortColumn = MessageColumns.SENDER_LIST;
                break;
            }
            case SORT_SUBJECT: {
                sortColumn = MessageColumns.SUBJECT + " COLLATE NOCASE";
                break;
            }
            case SORT_UNREAD: {
                sortColumn = MessageColumns.READ;
                break;
            }
            case SORT_DATE:
            default: {
                sortColumn = MessageColumns.DATE;
            }
        }

        String sortDirection = (mSortAscending) ? " ASC" : " DESC";
        String secondarySort;
        if (mSortType == SortType.SORT_DATE || mSortType == SortType.SORT_ARRIVAL) {
            secondarySort = "";
        } else {
            secondarySort = MessageColumns.DATE + ((mSortDateAscending) ? " ASC, " : " DESC, ");
        }

        String sortOrder = sortColumn + sortDirection + ", " + secondarySort +
                MessageColumns.ID + " DESC";
        return sortOrder;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": loaderId " + loader.getId() + " data: " + data);
        mAdapter.changeCursor(data);
        mAdapter.notifyDataSetChanged();

        //DIMA TODO: recalculate unread message count

        updateTitle();
    }

    public boolean isLoadFinished() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return true;
    }

    /**
     * Close the context menu when the message it was opened for is no longer in the message list.
     */
    private void updateContextMenu(Cursor cursor) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (mContextMenuUniqueId == 0) {
            return;
        }

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long uniqueId = cursor.getLong(mUniqueIdColumn);
            if (uniqueId == mContextMenuUniqueId) {
                return;
            }
        }

        mContextMenuUniqueId = 0;
        Activity activity = getActivity();
        if (activity != null) {
            activity.closeContextMenu();
        }*/
    }

    private void cleanupSelected(Cursor cursor) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   if (mSelected.size() == 0) {
            return;
        }

        Set<Long> selected = new HashSet<Long>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            long uniqueId = cursor.getLong(mUniqueIdColumn);
            if (mSelected.contains(uniqueId)) {
                selected.add(uniqueId);
            }
        }

        mSelected = selected;*/
    }

    /**
     * Starts or finishes the action mode when necessary.
     */
    private void resetActionMode() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (mSelected.size() == 0) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
            return;
        }

        if (mActionMode == null) {
            mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
        }

        recalculateSelectionCount();
        updateActionModeTitle();*/
    }

    /**
     * Recalculates the selection count.
     *
     * <p>
     * For non-threaded lists this is simply the number of visibly selected messages. If threaded
     * view is enabled this method counts the number of messages in the selected threads.
     * </p>
     */
    private void recalculateSelectionCount() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    /*    if (!mThreadedList) {
            mSelectedCount = mSelected.size();
            return;
        }

        mSelectedCount = 0;
        for (int i = 0, end = mAdapter.getCount(); i < end; i++) {
            Cursor cursor = (Cursor) mAdapter.getItem(i);
            long uniqueId = cursor.getLong(mUniqueIdColumn);

            if (mSelected.contains(uniqueId)) {
                int threadCount = cursor.getInt(THREAD_COUNT_COLUMN);
                mSelectedCount += (threadCount > 1) ? threadCount : 1;
            }
        }*/
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  mSelected.clear();
        mAdapter.swapCursor(null);
    }

    private Account getAccountFromCursor(Cursor cursor) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
        return mPreferences.getAccount(accountUuid);*/
        return null;
    }

    private void remoteSearchFinished() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      //  mRemoteSearchFuture = null;
    }

    /**
     * Mark a message as 'active'.
     *
     * <p>
     * The active message is the one currently displayed in the message view portion of the split
     * view.
     * </p>
     */
    public void setSelectedMsgId(Integer id/*MessageReference messageReference*/) {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": messageReference " + id);
        mActiveMsgId = id;

        // Reload message list with modified query that always includes the active message
        if (isAdded()) {
            restartLoader();
        }

        // Redraw list immediately
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public boolean isSingleAccountMode() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // mSingleAccountMode;
    }

    public boolean isSingleFolderMode() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // mSingleFolderMode;
    }

    public boolean isInitialized() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // mInitialized;
    }

    public boolean isMarkAllAsReadSupported() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; // (isSingleAccountMode() && isSingleFolderMode());
    }

    public boolean isCheckMailSupported() {
        Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return false; /* (mAllAccounts || !isSingleAccountMode() || !isSingleFolderMode() ||
                isRemoteFolder());*/
    }
/*
private boolean isCheckMailAllowed() {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    return false; // (!isManualSearch() && isCheckMailSupported());
}

private boolean isPullToRefreshAllowed() {
    Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
    return false; // (isRemoteSearchAllowed() || isCheckMailAllowed());
}
    */
}
