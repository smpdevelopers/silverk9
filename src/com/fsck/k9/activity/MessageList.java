package com.fsck.k9.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;
import com.fsck.k9.crypto.PgpData;
import com.fsck.k9.fragment.MessageListFragment;
import com.fsck.k9.fragment.MessageListFragment.MessageListFragmentListener;
import com.fsck.k9.fragment.MessageViewFragment;
import com.fsck.k9.fragment.MessageViewFragment.MessageViewFragmentListener;
import com.fsck.k9.mail.Message;
import com.fsck.k9.view.MessageHeader;
import com.fsck.k9.view.ViewSwitcher;
import com.fsck.k9.view.ViewSwitcher.OnSwitchCompleteListener;

import java.util.ArrayList;


/**
 * MessageList is the primary user interface for the program. This Activity
 * shows a list of messages.
 * From this Activity the user can perform all standard message operations.
 */
public class MessageList extends K9FragmentActivity implements MessageListFragmentListener,
        MessageViewFragmentListener, OnBackStackChangedListener, OnSwipeGestureListener,
        OnSwitchCompleteListener, ActionBar.OnNavigationListener{

    final private static boolean DEBUG = true;
    final private static String TAG = "MessageList";

    // for this activity
    private static final String EXTRA_MESSAGE_ID = "message_id";
    private static final String STATE_DISPLAY_MODE = "displayMode";

    public enum SortType {
        SORT_DATE(R.string.sort_earliest_first, R.string.sort_latest_first, false),
        SORT_ARRIVAL(R.string.sort_earliest_first, R.string.sort_latest_first, false),
        SORT_SUBJECT(R.string.sort_subject_alpha, R.string.sort_subject_re_alpha, true),
        SORT_SENDER(R.string.sort_sender_alpha, R.string.sort_sender_re_alpha, true),
        SORT_UNREAD(R.string.sort_unread_first, R.string.sort_unread_last, true),
        SORT_FLAGGED(R.string.sort_flagged_first, R.string.sort_flagged_last, true),
        SORT_ATTACHMENT(R.string.sort_attach_first, R.string.sort_unattached_first, true);

        private int ascendingToast;

        private int descendingToast;
        private boolean defaultAscending;
        SortType(int ascending, int descending, boolean ndefaultAscending) {
            ascendingToast = ascending;
            descendingToast = descending;
            defaultAscending = ndefaultAscending;
        }

        public int getToast(boolean ascending) {
            return (ascending) ? ascendingToast : descendingToast;
        }

        public boolean isDefaultAscending() {
            return defaultAscending;
        }

    }


    private enum DisplayMode {
        MESSAGE_LIST,
        MESSAGE_VIEW,
        SPLIT_VIEW;
    }
    private ActionBar mActionBar;

    //DIMA TODO: add updating unread messages when messageListView has been updated
    private TextView mActionBarUnread;
    private Menu mMenu;
    private ViewGroup mMessageViewContainer;

    private View mMessageViewPlaceHolder;
    private MessageListFragment mMessageListFragment;

    private MessageViewFragment mMessageViewFragment;
    private int mFirstBackStackId = -1;

    private DisplayMode mDisplayMode;
    private Integer mMessageId;

    private TitleNavigationAdapter adapter;

    /**
     * {@code true} when the message list was displayed once. This is used in
     * {@link #onBackPressed()} to decide whether to go from the message view to the message list or
     * finish the activity.
     */
    private ProgressBar mActionBarProgress;
    private View mActionButtonIndeterminateProgress;
    private boolean mMessageListWasDisplayed = false;
    private ViewSwitcher mViewSwitcher;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": savedInstanceState " + savedInstanceState);

        if (useSplitView()) {
            setContentView(R.layout.split_message_list);
            if (DEBUG) Log.d("MessageList", "onCreate - landscape");
        } else {
            setContentView(R.layout.message_list);
            if (DEBUG) Log.d("MessageList", "onCreate");
            mViewSwitcher = (ViewSwitcher) findViewById(R.id.container);
            mViewSwitcher.setFirstInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
            mViewSwitcher.setFirstOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
            mViewSwitcher.setSecondInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
            mViewSwitcher.setSecondOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
            mViewSwitcher.setOnSwitchCompleteListener(this);
        }

        // Enable gesture detection for MessageLists
        setupGestureDetector(this);

        initializeActionBar();
        findFragments();
        initializeDisplayMode(savedInstanceState);
        initializeLayout();
        initializeFragments();
        displayViews();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": intent " + intent);

        setIntent(intent);

        if (mFirstBackStackId >= 0) {
            getSupportFragmentManager().popBackStackImmediate(mFirstBackStackId,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mFirstBackStackId = -1;
        }
        removeMessageListFragment();
        removeMessageViewFragment();

        mMessageId = null;


        initializeDisplayMode(null);
        initializeFragments();
        displayViews();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": itemPosition " + itemPosition + "; itemId " + itemId);

        return false;
    }

    private class SpinnerNavItem {

        private String title;
        private int icon;

        public SpinnerNavItem(String title, int icon){
            this.title = title;
            this.icon = icon;
        }

        public String getTitle(){
            return this.title;
        }

        public int getIcon(){
            return this.icon;
        }
    }

    private class TitleNavigationAdapter extends BaseAdapter {

        private ImageView imgIcon;
        private TextView txtTitle;
        private ArrayList<SpinnerNavItem> spinnerNavItem;
        private Context context;

        public TitleNavigationAdapter(Context context,
                                      ArrayList<SpinnerNavItem> spinnerNavItem) {
            this.spinnerNavItem = spinnerNavItem;
            this.context = context;
        }

        @Override
        public int getCount() {
            return spinnerNavItem.size();
        }

        @Override
        public Object getItem(int index) {
            return spinnerNavItem.get(index);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater)
                        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                convertView = mInflater.inflate(R.layout.list_item_title_navigation, null);
            }

            imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
            txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);

            imgIcon.setImageResource(spinnerNavItem.get(position).getIcon());
            imgIcon.setVisibility(View.GONE);
            txtTitle.setText(spinnerNavItem.get(position).getTitle());
            return convertView;
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater)
                        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                convertView = mInflater.inflate(R.layout.list_item_title_navigation, null);
            }

            imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
            txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);

            imgIcon.setImageResource(spinnerNavItem.get(position).getIcon());
            txtTitle.setText(spinnerNavItem.get(position).getTitle());
            return convertView;
        }

    }

    private void initializeActionBar() {
        mActionBar = getSupportActionBar();

        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayList<SpinnerNavItem> navSpinner = new ArrayList<SpinnerNavItem>();
        navSpinner.add(new SpinnerNavItem("Inbox", R.drawable.ic_folder_outbox_holo_light));
        navSpinner.add(new SpinnerNavItem("Sent", R.drawable.ic_folder_sent_holo_light));

        // title drop down adapter
        adapter = new TitleNavigationAdapter(getApplicationContext(), navSpinner);

        // assigning the spinner navigation
        mActionBar.setListNavigationCallbacks(adapter, this);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.actionbar_custom);

        View customView = mActionBar.getCustomView();
        mActionBarUnread = (TextView) customView.findViewById(R.id.actionbar_unread_count);
    }

    /**
     * Get references to existing fragments if the activity was restarted.
     */
    private void findFragments() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        mMessageListFragment = (MessageListFragment) fragmentManager.findFragmentById(
                R.id.message_list_container);
        mMessageViewFragment = (MessageViewFragment) fragmentManager.findFragmentById(
                R.id.message_view_container);
        if (DEBUG) Log.d(TAG, "findFragment: mMessageListFragment: " + mMessageListFragment +
                    "; mMessageViewFragment: " + mMessageViewFragment);
    }

    /**
     * Create fragment instances if necessary.
     *
     * @see #findFragments()
     */
    private void initializeFragments() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        boolean hasMessageListFragment = (mMessageListFragment != null);

        if (!hasMessageListFragment) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mMessageListFragment = MessageListFragment.newInstance();
            ft.add(R.id.message_list_container, mMessageListFragment);
            ft.commit();
        }

        // Check if the fragment wasn't restarted and has a MessageReference in the arguments. If
        // so, open the referenced message.
        if (!hasMessageListFragment && mMessageViewFragment == null &&
                mMessageId != null) {
            openMessage(mMessageId);
        }
    }

    /**
     * Set the initial display mode (message list, message view, or split view).
     *
     * <p><strong>Note:</strong>
     * This method has to be called after {@link #findFragments()} because the result depends on
     * the availability of a {@link MessageViewFragment} instance.
     * </p>
     *
     * @param savedInstanceState
     *         The saved instance state that was passed to the activity as argument to
     *         {@link #onCreate(Bundle)}. May be {@code null}.
     */
    private void initializeDisplayMode(Bundle savedInstanceState) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": savedInstanceState " + savedInstanceState);
        if (useSplitView()) {
            mDisplayMode = DisplayMode.SPLIT_VIEW;
            if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                    + ": displayMode " + mDisplayMode);
            return;
        }

        if (savedInstanceState != null) {
            DisplayMode savedDisplayMode =
                    (DisplayMode) savedInstanceState.getSerializable(STATE_DISPLAY_MODE);
            if (savedDisplayMode != DisplayMode.SPLIT_VIEW) {
                mDisplayMode = savedDisplayMode;
                if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                        + ": displayMode " + mDisplayMode);
                return;
            }
        }

        if (mMessageViewFragment != null || mMessageId != null) {
            mDisplayMode = DisplayMode.MESSAGE_VIEW;
        } else {
            mDisplayMode = DisplayMode.MESSAGE_LIST;
        }
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": displayMode " + mDisplayMode);
    }

    private boolean useSplitView() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        return (getResources().getConfiguration().ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation);
    }

    private void initializeLayout() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mMessageViewContainer = (ViewGroup) findViewById(R.id.message_view_container);
        mMessageViewPlaceHolder = getLayoutInflater().inflate(R.layout.empty_message_view, null);
    }

    private void displayViews() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": mode " + mDisplayMode);
        switch (mDisplayMode) {
            case MESSAGE_LIST: {
                showMessageList();
                break;
            }
            case MESSAGE_VIEW: {
                showMessageView();
                break;
            }
            case SPLIT_VIEW: {
                mMessageListWasDisplayed = true;
                if (mMessageViewFragment == null && mMessageId == null) {
                    showMessageViewPlaceHolder();
                } else {
                    if (mMessageId != null) {
                        openMessage(mMessageId);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onPause();
    }

    @Override
    public void onResume() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        displayViews();

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onSaveInstanceState(outState);

        if(mDisplayMode == null) {
            if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
            + ": mDisplayMode is null!!!!");
        }
        outState.putSerializable(STATE_DISPLAY_MODE, mDisplayMode);
        if(mMessageId != null) {
            outState.putInt(EXTRA_MESSAGE_ID, mMessageId);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": savedInstanceState " + savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);

        mDisplayMode = (DisplayMode) savedInstanceState.getSerializable(STATE_DISPLAY_MODE);
        mMessageId = savedInstanceState.getInt(EXTRA_MESSAGE_ID);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": event " + event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW && mMessageListWasDisplayed) {
            showMessageList();
        } else {
            super.onBackPressed();
        }
    }

    //@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": item " + item);
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            goBack();
            return true;
        }
        else if (itemId == R.id.compose) {
            mMessageListFragment.onCompose();
            return true;
        }
        else if (itemId == R.id.toggle_message_view_theme) {
            onToggleTheme();
            return true;
        }
        //Message list
        else if (itemId == R.id.check_mail) {
            mMessageListFragment.checkMail();
            return true;
        }
        else if (itemId == R.id.set_sort_date) {
            mMessageListFragment.changeSort(SortType.SORT_DATE);
            return true;
        }
        else if (itemId == R.id.set_sort_arrival) {
            mMessageListFragment.changeSort(SortType.SORT_ARRIVAL);
            return true;
        }
        else if (itemId == R.id.set_sort_subject) {
            mMessageListFragment.changeSort(SortType.SORT_SUBJECT);
            return true;
        }
        else if (itemId == R.id.set_sort_sender) {
            mMessageListFragment.changeSort(SortType.SORT_SENDER);
            return true;
        }
        else if (itemId == R.id.set_sort_flag) {
            mMessageListFragment.changeSort(SortType.SORT_FLAGGED);
            return true;
        }
        else if (itemId == R.id.set_sort_unread) {
            mMessageListFragment.changeSort(SortType.SORT_UNREAD);
            return true;
        }
        else if (itemId == R.id.set_sort_attach) {
            mMessageListFragment.changeSort(SortType.SORT_ATTACHMENT);
            return true;
        }
        else if (itemId == R.id.select_all) {
            mMessageListFragment.selectAll();
            return true;
        }
        else if (itemId == R.id.search) {
            return true;
        }
        else if (itemId == R.id.search_remote) {
            return true;
        }
        else if (itemId == R.id.mark_all_as_read) {
            return true;
        }
        else if (itemId == R.id.show_folder_list) {
            return true;
        }
        //Message View
        else if (itemId == R.id.next_message) {
            showNextMessage();
            return true;
        }
        else if (itemId == R.id.previous_message) {
            showPreviousMessage();
            return true;
        }
        else if (itemId == R.id.delete) {
            mMessageViewFragment.onDelete();
            return true;
        }
        else if (itemId == R.id.reply) {
            mMessageViewFragment.onReply();
            return true;
        }
        else if (itemId == R.id.forward) {
            mMessageViewFragment.onForward();
            return true;
        }
        else if (itemId == R.id.toggle_unread) {
            mMessageViewFragment.onToggleRead();
            return true;
        }
        else if (itemId == R.id.spam) {
            //DIMA TODO: remove
            return true;
        }
        else if (itemId == R.id.move) {
            //DIMA TODO: remove
            return true;
        }
        else if (itemId == R.id.copy) {
            //DIMA TODO: remove
            return true;
        }
        else if (itemId == R.id.select_text) {
            //DIMA TODO: remove
            return true;
        }
        else if (itemId == R.id.send_messages) {
                mMessageListFragment.onSendPendingMessages();
                return true;
        }
        else if (itemId == R.id.folder_settings) {
            //DIMA TODO: remove
                return true;
        }
        else if (itemId == R.id.expunge) {
            mMessageListFragment.onExpunge();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    //@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": menu " + menu);
        getSupportMenuInflater().inflate(R.menu.message_list_option, menu);
        mMenu = menu;
        return true;
    }

    //@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": menu " + menu);
        configureMenu(menu);
        return true;
    }

    /**
     * Hide menu items not appropriate for the current context.
     *
     * <p><strong>Note:</strong>
     * Please adjust the comments in {@code res/menu/message_list_option.xml} if you change the
     * visibility of a menu item in this method.
     * </p>
     *
     * @param menu
     *         The {@link Menu} instance that should be modified. May be {@code null}; in that case
     *         the method does nothing and immediately returns.
     */
    private void configureMenu(Menu menu) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": menu " + menu);
        if (menu == null) {
            return;
        }

        // Set visibility of account/folder settings menu items
        if (mMessageListFragment == null) {
            menu.findItem(R.id.folder_settings).setVisible(false);
        } else {
            menu.findItem(R.id.folder_settings).setVisible(
                    mMessageListFragment.isSingleFolderMode());
        }

        /*
         * Set visibility of menu items related to the message view
         */

        if (mDisplayMode == DisplayMode.MESSAGE_LIST
                || mMessageViewFragment == null
                || !mMessageViewFragment.isInitialized()) {
            menu.findItem(R.id.next_message).setVisible(false);
            menu.findItem(R.id.previous_message).setVisible(false);
            menu.findItem(R.id.single_message_options).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            menu.findItem(R.id.compose).setVisible(false);
            menu.findItem(R.id.archive).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.spam).setVisible(false);
            menu.findItem(R.id.toggle_unread).setVisible(false);
            menu.findItem(R.id.select_text).setVisible(false);
            menu.findItem(R.id.toggle_message_view_theme).setVisible(false);
        } else {
            // hide prev/next buttons in split mode
            if (mDisplayMode != DisplayMode.MESSAGE_VIEW) {
                menu.findItem(R.id.next_message).setVisible(false);
                menu.findItem(R.id.previous_message).setVisible(false);
            } else {
                Integer msgId = mMessageViewFragment.getMsgId();
                boolean initialized = (mMessageListFragment != null &&
                        mMessageListFragment.isLoadFinished());
                boolean canDoPrev = (initialized && !mMessageListFragment.isFirst(msgId));
                boolean canDoNext = (initialized && !mMessageListFragment.isLast(msgId));

                MenuItem prev = menu.findItem(R.id.previous_message);
                prev.setEnabled(canDoPrev);
                prev.getIcon().setAlpha(canDoPrev ? 255 : 127);

                MenuItem next = menu.findItem(R.id.next_message);
                next.setEnabled(canDoNext);
                next.getIcon().setAlpha(canDoNext ? 255 : 127);
            }

            MenuItem toggleTheme = menu.findItem(R.id.toggle_message_view_theme);
            if (K9.useFixedMessageViewTheme()) {
                toggleTheme.setVisible(false);
            } else {
                // Set title of menu item to switch to dark/light theme
                if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
                    toggleTheme.setTitle(R.string.message_view_theme_action_light);
                } else {
                    toggleTheme.setTitle(R.string.message_view_theme_action_dark);
                }
                toggleTheme.setVisible(true);
            }

            // Set title of menu item to toggle the read state of the currently displayed message
            if (mMessageViewFragment.isMessageRead()) {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_unread_action);
            } else {
                menu.findItem(R.id.toggle_unread).setTitle(R.string.mark_as_read_action);
            }

            // Jellybean has built-in long press selection support
            menu.findItem(R.id.select_text).setVisible(Build.VERSION.SDK_INT < 16);

            menu.findItem(R.id.delete).setVisible(K9.isMessageViewDeleteActionVisible());

            /*
             * Set visibility of copy, move, archive, spam in action bar and refile submenu
             */
           /* if (mMessageViewFragment.isCopyCapable()) {
                menu.findItem(R.id.copy).setVisible(K9.isMessageViewCopyActionVisible());
                menu.findItem(R.id.refile_copy).setVisible(true);
            } else {*/
                menu.findItem(R.id.copy).setVisible(false);
           // }

           /* if (mMessageViewFragment.isMoveCapable()) {
                boolean canMessageBeArchived = mMessageViewFragment.canMessageBeArchived();
                boolean canMessageBeMovedToSpam = mMessageViewFragment.canMessageBeMovedToSpam();

                menu.findItem(R.id.move).setVisible(K9.isMessageViewMoveActionVisible());
                menu.findItem(R.id.archive).setVisible(canMessageBeArchived &&
                        K9.isMessageViewArchiveActionVisible());
                menu.findItem(R.id.spam).setVisible(canMessageBeMovedToSpam &&
                        K9.isMessageViewSpamActionVisible());

                menu.findItem(R.id.refile_move).setVisible(true);
                menu.findItem(R.id.refile_archive).setVisible(canMessageBeArchived);
                menu.findItem(R.id.refile_spam).setVisible(canMessageBeMovedToSpam);
            } else {*/
                menu.findItem(R.id.move).setVisible(false);
                menu.findItem(R.id.archive).setVisible(false);
                menu.findItem(R.id.spam).setVisible(false);
           // }
        }


        /*
         * Set visibility of menu items related to the message list
         */

        // Hide both search menu items by default and enable one when appropriate
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.search_remote).setVisible(false);

        if (mDisplayMode == DisplayMode.MESSAGE_VIEW || mMessageListFragment == null ||
                !mMessageListFragment.isInitialized()) {
            menu.findItem(R.id.check_mail).setVisible(false);
            menu.findItem(R.id.set_sort).setVisible(false);
            menu.findItem(R.id.select_all).setVisible(false);
            menu.findItem(R.id.send_messages).setVisible(false);
            menu.findItem(R.id.expunge).setVisible(false);
            menu.findItem(R.id.mark_all_as_read).setVisible(false);
            menu.findItem(R.id.show_folder_list).setVisible(false);
        } else {
            menu.findItem(R.id.set_sort).setVisible(true);
            menu.findItem(R.id.select_all).setVisible(true);
            menu.findItem(R.id.compose).setVisible(true);
            menu.findItem(R.id.mark_all_as_read).setVisible(
                    mMessageListFragment.isMarkAllAsReadSupported());

            if (!mMessageListFragment.isSingleAccountMode()) {
                menu.findItem(R.id.expunge).setVisible(false);
                menu.findItem(R.id.send_messages).setVisible(false);
                menu.findItem(R.id.show_folder_list).setVisible(false);
            } else {
                menu.findItem(R.id.send_messages).setVisible(mMessageListFragment.isOutbox());
                menu.findItem(R.id.expunge).setVisible(mMessageListFragment.isRemoteFolder() &&
                        mMessageListFragment.isAccountExpungeCapable());
                menu.findItem(R.id.show_folder_list).setVisible(true);
            }

            menu.findItem(R.id.check_mail).setVisible(mMessageListFragment.isCheckMailSupported());

            // If this is an explicit local search, show the option to search on the server
            if (!mMessageListFragment.isRemoteSearch() &&
                    mMessageListFragment.isRemoteSearchAllowed()) {
                menu.findItem(R.id.search_remote).setVisible(true);
            } else if (!mMessageListFragment.isManualSearch()) {
                menu.findItem(R.id.search).setVisible(true);
            }
        }
    }

    protected void onAccountUnavailable() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  finish();
        // TODO inform user about account unavailability using Toast
        Accounts.listAccounts(this);*/
    }

    public void setActionBarTitle(String title) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": title " + title);
       // mActionBarTitle.setText(title);
    }

    public void setActionBarSubTitle(String subTitle) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": subTitle " + subTitle);
      //  mActionBarSubTitle.setText(subTitle);
    }

    public void setActionBarUnread(int unread) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": unread " + unread);
        if (unread == 0) {
            mActionBarUnread.setVisibility(View.GONE);
        } else {
            mActionBarUnread.setVisibility(View.VISIBLE);
            mActionBarUnread.setText(Integer.toString(unread));
        }
    }

    @Override
    public void setMessageListTitle(String title) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": title " + title);
      //  setActionBarTitle(title);
    }

    @Override
    public void setMessageListSubTitle(String subTitle) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": subTitle " + subTitle);
      //  setActionBarSubTitle(subTitle);
    }

    @Override
    public void setUnreadCount(int unread) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": unread " + unread);
        setActionBarUnread(unread);
    }

    @Override
    public void setMessageListProgress(int progress) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": progress " + progress);
      //  setSupportProgress(progress);
    }

    @Override
    public void openMessage(Integer id) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": messageReference " + id);
        mMessageId = id;

        mMessageViewContainer.removeView(mMessageViewPlaceHolder);
        if(null != mMessageViewContainer && 0 < mMessageViewContainer.getChildCount()) {
            mMessageViewContainer.removeAllViews();
        }

        if (mMessageListFragment != null) {
            mMessageListFragment.setSelectedMsgId(id);
        }

        MessageViewFragment fragment = MessageViewFragment.newInstance(id);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.message_view_container, fragment);
        mMessageViewFragment = fragment;
        ft.commit();

        if (mDisplayMode != DisplayMode.SPLIT_VIEW) {
            showMessageView();
        }
    }

    @Override
    public void onResendMessage(Message message) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + message);
        MessageCompose.actionEditDraft(this, message.makeMessageReference());
    }

    @Override
    public void onForward(Message message) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + message);
      //  MessageCompose.actionForward(this, message.getFolder().getAccount(), message, null);
    }

    @Override
    public void onReply(Message message) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + message);
      //  MessageCompose.actionReply(this, message.getFolder().getAccount(), message, false, null);
    }

    @Override
    public void onReplyAll(Message message) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + message);
      //  MessageCompose.actionReply(this, message.getFolder().getAccount(), message, true, null);
    }

    @Override
    public void showMoreFromSameSender(String senderAddress) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": senderAddress " + senderAddress);
       /* LocalSearch tmpSearch = new LocalSearch("From " + senderAddress);
        tmpSearch.addAccountUuids(mSearch.getAccountUuids());
        tmpSearch.and(Searchfield.SENDER, senderAddress, Attribute.CONTAINS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, false, false);

        addMessageListFragment(fragment, true);*/
    }

    @Override
    public void onCompose() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        //DIMA TODO: add calling that activity
        //MessageCompose.actionCompose(this, null);
    }

    @Override
    public void onBackStackChanged() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        findFragments();

        if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
            showMessageViewPlaceHolder();
        }

        configureMenu(mMenu);
    }

    @Override
    public void onSwipeRightToLeft(MotionEvent e1, MotionEvent e2) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": e1 " + e1 + "; e2 " + e2);
      /*  if (mMessageListFragment != null && mDisplayMode != DisplayMode.MESSAGE_VIEW) {
            mMessageListFragment.onSwipeRightToLeft(e1, e2);
        }*/
    }

    @Override
    public void onSwipeLeftToRight(MotionEvent e1, MotionEvent e2) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": e1 " + e1 + "; e2 " + e2);
      /*  if (mMessageListFragment != null && mDisplayMode != DisplayMode.MESSAGE_VIEW) {
            mMessageListFragment.onSwipeLeftToRight(e1, e2);
        }*/
    }

   /* private final class StorageListenerImplementation implements StorageManager.StorageListener {
        @Override
        public void onUnmount(String providerId) {
            if (mAccount != null && providerId.equals(mAccount.getLocalStorageProviderId())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onAccountUnavailable();
                    }
                });
            }
        }

        @Override
        public void onMount(String providerId) {
            // no-op
        }
    }
*/

    private void addMessageListFragment(MessageListFragment fragment, boolean addToBackStack) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": fragment " + fragment + "; addToBackStack " + addToBackStack);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ft.replace(R.id.message_list_container, fragment);
        if (addToBackStack)
            ft.addToBackStack(null);

        mMessageListFragment = fragment;

        int transactionId = ft.commit();
        if (transactionId >= 0 && mFirstBackStackId < 0) {
            mFirstBackStackId = transactionId;
        }
    }

    @Override
    public boolean startSearch(Account account, String folderName) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": account " + account + "; folderName " + folderName);
       /* // If this search was started from a MessageList of a single folder, pass along that folder info
        // so that we can enable remote search.
        if (account != null && folderName != null) {
            final Bundle appData = new Bundle();
            appData.putString(EXTRA_SEARCH_ACCOUNT, account.getUuid());
            appData.putString(EXTRA_SEARCH_FOLDER, folderName);
            startSearch(null, false, appData, false);
        } else {
            // TODO Handle the case where we're searching from within a search result.
            startSearch(null, false, null, false);
        }
*/
        return true;
    }

    @Override
    public void showThread(Account account, String folderName, long threadRootId) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": account " + account + "; folderName " + folderName + "; threadRootId " + threadRootId);
       /* showMessageViewPlaceHolder();

        LocalSearch tmpSearch = new LocalSearch();
        tmpSearch.addAccountUuid(account.getUuid());
        tmpSearch.and(Searchfield.THREAD_ID, String.valueOf(threadRootId), Attribute.EQUALS);

        MessageListFragment fragment = MessageListFragment.newInstance(tmpSearch, true, false);
        addMessageListFragment(fragment, true);*/
    }

    private void showMessageViewPlaceHolder() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        removeMessageViewFragment();

        // Add placeholder view if necessary
        if (mMessageViewPlaceHolder.getParent() == null) {
            mMessageViewContainer.addView(mMessageViewPlaceHolder);
        }

        mMessageListFragment.setSelectedMsgId(null);
    }

    /**
     * Remove MessageViewFragment if necessary.
     */
    private void removeMessageViewFragment() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        if (mMessageViewFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(mMessageViewFragment);
            mMessageViewFragment = null;
            ft.commit();

            showDefaultTitleView();
        }
    }

    private void removeMessageListFragment() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(mMessageListFragment);
        mMessageListFragment = null;
        ft.commit();
    }

    @Override
    public void remoteSearchStarted() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        // Remove action button for remote search
        configureMenu(mMenu);
    }

    @Override
    public void goBack() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
            showMessageList();
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else if (mMessageListFragment.isManualSearch()) {
            finish();
        } else {
            //DIMA TODO:show folder list should be add for moving between inbox\sent
            //onShowFolderList();
        }
    }

    @Override
    public void displayMessageSubject(String subject) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": subject " + subject);
        //DIMA TODO: install 'inbox'\'sent' title
      /*  if (mDisplayMode == DisplayMode.MESSAGE_VIEW) {
            mActionBarSubject.setText(subject);
        }*/
    }

    @Override
    public void onReply(Message message, PgpData pgpData) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + message + "; pgpData " + pgpData);
       // MessageCompose.actionReply(this, mAccount, message, false, pgpData.getDecryptedData());
    }

    @Override
    public void onReplyAll(Message message, PgpData pgpData) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + message + "; pgpData " + pgpData);
       // MessageCompose.actionReply(this, mAccount, message, true, pgpData.getDecryptedData());
    }

    @Override
    public void onForward(Message mMessage, PgpData mPgpData) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": message " + mMessage + "; pgpData " + mPgpData);
       // MessageCompose.actionForward(this, mAccount, mMessage, mPgpData.getDecryptedData());
    }

    @Override
    public void showHoldMessage() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mMessageId = null;
        showMessageViewPlaceHolder();
    }

    @Override
    public void showNextMessageOrReturn() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  if (K9.messageViewReturnToList() || !showLogicalNextMessage()) {
            if (mDisplayMode == DisplayMode.SPLIT_VIEW) {
                showMessageViewPlaceHolder();
            } else {
                showMessageList();
            }
        }*/
    }

    /**
     * Shows the next message in the direction the user was displaying messages.
     *
     * @return {@code true}
     */
    private boolean showLogicalNextMessage() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   boolean result = false;
        if (mLastDirection == NEXT) {
            result = showNextMessage();
        } else if (mLastDirection == PREVIOUS) {
            result = showPreviousMessage();
        }

        if (!result) {
            result = showNextMessage() || showPreviousMessage();
        }

        return result;*/return true;
    }

    @Override
    public void setProgress(boolean enable) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": enable " + enable);
        setSupportProgressBarIndeterminateVisibility(enable);
    }

    @Override
    public void messageHeaderViewAvailable(MessageHeader header) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": header " + header);
      //  mActionBarSubject.setMessageHeader(header);
    }

    private boolean showNextMessage() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*  MessageReference ref = mMessageViewFragment.getMessageReference();
        if (ref != null) {
            if (mMessageListFragment.openNext(ref)) {
                mLastDirection = NEXT;
                return true;
            }
        }*/
        return false;
    }

    private boolean showPreviousMessage() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
     /*   MessageReference ref = mMessageViewFragment.getMessageReference();
        if (ref != null) {
            if (mMessageListFragment.openPrevious(ref)) {
                mLastDirection = PREVIOUS;
                return true;
            }
        }*/
        return false;
    }

    private void showMessageList() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mMessageListWasDisplayed = true;
        mDisplayMode = DisplayMode.MESSAGE_LIST;
        mViewSwitcher.showFirstView();

        mMessageListFragment.setSelectedMsgId(null);

        showDefaultTitleView();
        configureMenu(mMenu);
    }

    private void showMessageView() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mDisplayMode = DisplayMode.MESSAGE_VIEW;

        if (!mMessageListWasDisplayed) {
            mViewSwitcher.setAnimateFirstView(false);
        }
        mViewSwitcher.showSecondView();

        showMessageTitleView();
        configureMenu(mMenu);
    }

    @Override
    public void updateMenu() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        invalidateOptionsMenu();
    }

    @Override
    public void disableDeleteAction() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        mMenu.findItem(R.id.delete).setEnabled(false);
    }

    private void onToggleTheme() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
       /* if (K9.getK9MessageViewTheme() == K9.Theme.DARK) {
            K9.setK9MessageViewThemeSetting(K9.Theme.LIGHT);
        } else {
            K9.setK9MessageViewThemeSetting(K9.Theme.DARK);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Context appContext = getApplicationContext();
                Preferences prefs = Preferences.getPreferences(appContext);
                Editor editor = prefs.getPreferences().edit();
                K9.save(editor);
                editor.commit();
            }
        }).start();

        restartActivity();*/
    }

    private void showDefaultTitleView() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
        + ": skipping");
       /* mActionBarMessageView.setVisibility(View.GONE);
        mActionBarMessageList.setVisibility(View.VISIBLE);

        if (mMessageListFragment != null) {
            mMessageListFragment.updateTitle();
        }

        mActionBarSubject.setMessageHeader(null);*/
    }

    private void showMessageTitleView() {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
      /*  mActionBarMessageList.setVisibility(View.GONE);
        mActionBarMessageView.setVisibility(View.VISIBLE);

        if (mMessageViewFragment != null) {
            displayMessageSubject(null);
            mMessageViewFragment.updateTitle();
        }*/
    }

    @Override
    public void onSwitchComplete(int displayedChild) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": displayedChild " + displayedChild);
       /* if (displayedChild == 0) {
            removeMessageViewFragment();
        }*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.d(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName()
                + ": requestCode " + requestCode + "; resultCode " + resultCode + "; data " + data);
      /*  super.onActivityResult(requestCode, resultCode, data);

        // handle OpenPGP results from PendingIntents in OpenPGP view
        // must be handled in this main activity, because startIntentSenderForResult() does not support Fragments
        MessageOpenPgpView openPgpView = (MessageOpenPgpView) findViewById(R.id.layout_decrypt_openpgp);
        if (openPgpView != null && openPgpView.handleOnActivityResult(requestCode, resultCode, data)) {
            return;
        }*/
    }
}
