package com.fsck.k9.activity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;

public class MessageReference implements Parcelable {
    private static final boolean DEBUG = true;
    public Integer _id;
    /**
     * Initialize an empty MessageReference.
     */
    public MessageReference() {
    }

    public MessageReference(final int id) {
        if (DEBUG) Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        _id = id;
    }
    /**DIMA TODO: remove that comment
     * Initialize a MessageReference from a seraialized identity.
     * @param identity Serialized identity.
     * @throws MessagingException On missing or corrupted identity.
     */
    public MessageReference(final Integer id) throws MessagingException {
        if (DEBUG) Log.e(Thread.currentThread().getStackTrace()[2].getClassName(), Thread.currentThread().getStackTrace()[2].getMethodName());
        // Can't be null and must be at least length one so we can check the version.
        if (id == null) {
            throw new MessagingException("Null or truncated MessageReference identity.");
        }
        _id = id;

/*
        // Version check.
        if (identity.charAt(0) == IDENTITY_VERSION_1.charAt(0)) {
            // Split the identity, stripping away the first two characters representing the version and delimiter.
            StringTokenizer tokens = new StringTokenizer(identity.substring(2), IDENTITY_SEPARATOR, false);
            if (tokens.countTokens() >= 3) {
                accountUuid = Utility.base64Decode(tokens.nextToken());
                folderName = Utility.base64Decode(tokens.nextToken());
                uid = Utility.base64Decode(tokens.nextToken());

                if (tokens.hasMoreTokens()) {
                    final String flagString = tokens.nextToken();
                    try {
                        flag = Flag.valueOf(flagString);
                    } catch (IllegalArgumentException ie) {
                        throw new MessagingException("Could not thaw message flag '" + flagString + "'", ie);
                    }
                }

                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Thawed " + toString());
            } else {
                throw new MessagingException("Invalid MessageReference in " + identity + " identity.");
            }
        }
        */
    }

    /**
     * Serialize this MessageReference for storing in a K9 identity.  This is a colon-delimited base64 string.
     *
     * @return Serialized string.
     */
 /*   public String toIdentityString() {
        StringBuilder refString = new StringBuilder();

        refString.append(IDENTITY_VERSION_1);
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Utility.base64Encode(accountUuid));
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Utility.base64Encode(folderName));
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Utility.base64Encode(uid));
        if (flag != null) {
            refString.append(IDENTITY_SEPARATOR);
            refString.append(flag.name());
        }

        return refString.toString();
    }
*/
    @Override
    public boolean equals(Object o) {
        if (o instanceof MessageReference == false) {
            return false;
        }
/*
        MessageReference other = (MessageReference)o;
        if ((accountUuid == other.accountUuid || (accountUuid != null && accountUuid.equals(other.accountUuid)))
                && (folderName == other.folderName || (folderName != null && folderName.equals(other.folderName)))
                && (uid == other.uid || (uid != null && uid.equals(other.uid)))) {
            return true;
        }
        */
        MessageReference other = (MessageReference)o;
        if(other._id == _id)
            return true;

        return false;
    }

    @Override
    public String toString() {
        return "MessageReference{" +
               "_id=" + _id +
               '}';
    }

    public Message restoreToLocalMessage(Context context) {
      /*  try {
            Account account = Preferences.getPreferences(context).getAccount(accountUuid);
            if (account != null) {
                Folder folder = account.getLocalStore().getFolder(folderName);
                if (folder != null) {
                    Message message = folder.getMessage(uid);
                    if (message != null) {
                        return message;
                    } else {
                        Log.d(K9.LOG_TAG, "Could not restore message, uid " + uid + " is unknown.");
                    }
                } else {
                    Log.d(K9.LOG_TAG, "Could not restore message, folder " + folderName + " is unknown.");
                }
            } else {
                Log.d(K9.LOG_TAG, "Could not restore message, account " + accountUuid + " is unknown.");
            }
        } catch (MessagingException e) {
            Log.w(K9.LOG_TAG, "Could not retrieve message for reference.", e);
        }
*/
        return null;
    }
/*
    public static final Creator<MessageReference> CREATOR = new Creator<MessageReference>() {
        @Override
        public MessageReference createFromParcel(Parcel source) {
            MessageReference ref = new MessageReference();
            ref.uid = source.readString();
            ref.accountUuid = source.readString();
            ref.folderName = source.readString();
            String flag = source.readString();
            if (flag != null) ref.flag = Flag.valueOf(flag);
            return ref;
        }

        @Override
        public MessageReference[] newArray(int size) {
            return new MessageReference[size];
        }
    };*/

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_id);
    }
}
