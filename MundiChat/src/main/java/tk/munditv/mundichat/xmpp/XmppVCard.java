package tk.munditv.mundichat.xmpp;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.util.ArrayList;

import tk.munditv.mundichat.utils.Logger;

public class XmppVCard {

    private static final String TAG = "XmppVCard";
    private static ArrayList<VCardInfo> vcards = new ArrayList<VCardInfo>();

    private XmppVCard() { }

    public static void setVCard(String account, VCard card) {
        Logger.debug(TAG, "setVCard(\"" + account + "\")");
        if (vcards.size() > 0) {
            for (VCardInfo mVCard : vcards) {
                if (mVCard.account.contains(account)) {
                    return;
                }
            }
        }
        VCardInfo mVCard = new VCardInfo();
        mVCard.account = account;
        mVCard.vcard = card;
        vcards.add(mVCard);
        Logger.debug(TAG, "size=" + vcards.size());
    }

    public static VCard getVCard(String account) {
        Logger.debug(TAG, "getVCard(\"" + account + "\")");
        if (vcards.size() == 0) return null;
        for (VCardInfo mVCard : vcards) {
            if (mVCard.account.contains(account)) {
               return mVCard.vcard;
            }
        }
        return null;
    }

    public int size() {
        return vcards.size();
    }

}
