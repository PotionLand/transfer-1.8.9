package us.hughmung.transfer;

import com.google.common.net.InternetDomainName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@SuppressWarnings("UnstableApiUsage")
public final class Verifier {

    private Verifier() {}

    public static Result verify(String current, String target) {
        String targetBase = getBaseDomain(target);
        String targetTxtRecords = resolveTXT(targetBase);

        if (targetTxtRecords == null) {
            return new Result(false, true);
        }

        String[] allowedDomains = targetTxtRecords.split(",");
        for (String allowedDomain : allowedDomains) {
            if (allowedDomain.equals("*") || allowedDomain.equals(current)) {
                return new Result(true, true);
            } else if (allowedDomain.startsWith("*.")) {
                String allowedBaseDomain = allowedDomain.substring(2);
                if (current.endsWith(allowedBaseDomain)) {
                    return new Result(true, true);
                }
            }
        }

        return new Result(true, false);
    }

    public static String getCurrentServerAddress() {
        ServerData serverData = Minecraft.getMinecraft().getCurrentServerData();
        return serverData.serverIP;
    }

    public static String getBaseDomain(String domain) {
        return InternetDomainName.from(domain).topPrivateDomain().toString();
    }

    public static String resolveTXT(String host) {
        String domain = getBaseDomain(host);

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");

        try {
            DirContext dirContext = new InitialDirContext(env);
            Attributes attributes = dirContext.getAttributes(domain, new String[]{"TXT"});
            Attribute txtRecords = attributes.get("TXT");

            if (txtRecords != null) {
                NamingEnumeration<?> enumeration = txtRecords.getAll();
                while (enumeration.hasMoreElements()) {
                    String record = (String) enumeration.nextElement();
                    if (record.startsWith("mc_transfer_accept_from=")) {
                        return record.substring(24);
                    }
                }
            }

            return null;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    public static final class Result {
        private final boolean hasTxtRecord, isTransferAllowed;

        Result(boolean hasTxtRecord, boolean isTransferAllowed) {
            this.hasTxtRecord = hasTxtRecord;
            this.isTransferAllowed = isTransferAllowed;
        }

        public boolean hasTxtRecord() {
            return hasTxtRecord;
        }

        public boolean isTransferAllowed() {
            return isTransferAllowed;
        }
    }
}
