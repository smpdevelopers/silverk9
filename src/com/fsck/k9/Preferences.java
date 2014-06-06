
package com.fsck.k9;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fsck.k9.mail.Store;
import com.fsck.k9.preferences.Editor;
import com.fsck.k9.preferences.Storage;

public class Preferences {

    /**
     * Immutable empty {@link Account} array
     */
    private static final Account[] EMPTY_ACCOUNT_ARRAY = new Account[0];

    private static Preferences preferences;

    public static synchronized Preferences getPreferences(Context context) {
        Context appContext = context.getApplicationContext();
        if (preferences == null) {
            preferences = new Preferences(appContext);
        }
        return preferences;
    }


    //DIMA TODO: #1 commented. In future DB will be rewrited & used saving messages
    //private Storage mStorage;
    private Map<String, Account> accounts = null;
    private List<Account> accountsInOrder = null;
    private Account newAccount;
    private Context mContext;

    private Preferences(Context context) {
        mContext = context;
        /*DIMA TODO: #1
        mStorage = Storage.getStorage(context);
        if (mStorage.size() == 0) {
            Log.i(K9.LOG_TAG, "Preferences storage is zero-size, importing from Android-style preferences");
            Editor editor = mStorage.edit();
            editor.copy(context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE));
            editor.commit();
        }*/
    }

    public synchronized void loadAccounts() {
        accounts = new HashMap<String, Account>();
        accountsInOrder = new LinkedList<Account>();
        Log.e("ERROR", "loadAccounts");

        String accountUuids = getPreferences().getString("accountUuids", null);
        if ((accountUuids != null) && (accountUuids.length() != 0)) {
            String[] uuids = accountUuids.split(",");
            for (String uuid : uuids) {
                Account newAccount = new Account(this, uuid);
                accounts.put(uuid, newAccount);
                accountsInOrder.add(newAccount);
            }
        }
        if ((newAccount != null) && newAccount.getAccountNumber() != -1) {
            accounts.put(newAccount.getUuid(), newAccount);
            accountsInOrder.add(newAccount);
            newAccount = null;
        }
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     * @return all accounts
     */
    public synchronized Account[] getAccounts() {
        /* DIMA TODO: #1
        if (accounts == null) {
            loadAccounts();
        }

        return accountsInOrder.toArray(EMPTY_ACCOUNT_ARRAY);*/
        return new Account[0];
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     * @return all accounts with {@link Account#isAvailable(Context)}
     */
    public synchronized Collection<Account> getAvailableAccounts() {
        Account[] allAccounts = getAccounts();
        Collection<Account> retval = new ArrayList<Account>(accounts.size());
        for (Account account : allAccounts) {
            if (account.isEnabled() && account.isAvailable(mContext)) {
                retval.add(account);
            }
        }

        return retval;
    }

    public synchronized Account getAccount(String uuid) {
        /*DIMA TODO: #1 (should be added getting account by ID or never use it)
        if (accounts == null) {
            loadAccounts();
        }
        Account account = accounts.get(uuid);
        return account;
        */
        return null;
    }

    public synchronized Account newAccount() {
        newAccount = new Account(mContext/*K9.app*/);
        accounts.put(newAccount.getUuid(), newAccount);
        accountsInOrder.add(newAccount);

        return newAccount;
    }

    public synchronized void deleteAccount(Account account) {
        if (accounts != null) {
            accounts.remove(account.getUuid());
        }
        if (accountsInOrder != null) {
            accountsInOrder.remove(account);
        }

        Store.removeAccount(account);

        account.deleteCertificates();
        account.delete(this);

        if (newAccount == account) {
            newAccount = null;
        }
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     */
    public Account getDefaultAccount() {
        Log.e("ERROR", "getDefaultAccount");
        String defaultAccountUuid = getPreferences().getString("defaultAccountUuid", null);
        Account defaultAccount = getAccount(defaultAccountUuid);

        if (defaultAccount == null) {
            Collection<Account> accounts = getAvailableAccounts();
            if (!accounts.isEmpty()) {
                defaultAccount = accounts.iterator().next();
                setDefaultAccount(defaultAccount);
            }
        }

        return defaultAccount;
    }

    public void setDefaultAccount(Account account) {
        Log.e("ERROR", "setDefaultAccount");
        getPreferences().edit().putString("defaultAccountUuid", account.getUuid()).commit();
    }

    public SharedPreferences getPreferences() {
        return null;
        //DIMA TODO: #1
        //return mStorage;
    }
}
