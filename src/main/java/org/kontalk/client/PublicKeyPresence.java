/*
 * Kontalk client common library
 * Copyright (C) 2014 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.client;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;


/** Packet extension for presence stanzas with public key. */
public class PublicKeyPresence implements PacketExtension {
    public static final String ELEMENT_NAME = PublicKeyPublish.ELEMENT_NAME;
    public static final String NAMESPACE = PublicKeyPublish.NAMESPACE;

    /** Public key data. */
    private final byte[] mKey;
    /** Public key fingerprint. */
    private final String mFingerprint;

    /** Base64-encoded public key (cached). */
    private String mEncodedKey;

    public PublicKeyPresence(String keydata) {
        this(keydata != null ? Base64.decode(keydata) : null, null);
        mEncodedKey = keydata;
    }

    public PublicKeyPresence(byte[] keydata) {
        this(keydata, null);
    }

    public PublicKeyPresence(String keydata, String fingerprint) {
        this(keydata != null ? Base64.decode(keydata) : null, fingerprint);
        mEncodedKey = keydata;
    }

    public PublicKeyPresence(byte[] keydata, String fingerprint) {
        mKey = keydata;
        mFingerprint = fingerprint;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public String getFingerprint() {
        return mFingerprint;
    }

    public byte[] getKey() {
        return mKey;
    }

    @Override
    public String toXML() {
        if (mEncodedKey == null && mKey != null)
            mEncodedKey = Base64.encodeToString(mKey);

        XmlStringBuilder buf = new XmlStringBuilder()
            .halfOpenElement(ELEMENT_NAME)
            .xmlnsAttribute(NAMESPACE)
            .rightAngleBracket();

        if (mEncodedKey != null) {
            buf.openElement("key")
                .append(mEncodedKey)
                .closeElement("key");
        }

        if (mFingerprint != null) {
            buf.openElement("print")
                .append(mFingerprint)
                .closeElement("print");
        }

        return buf.closeElement(ELEMENT_NAME)
            .toString();
    }

    public static class Provider extends PacketExtensionProvider<PublicKeyPresence> {

        @Override
        public PublicKeyPresence parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
            String key = null, print = null;
            boolean in_key = false, in_print = false, done = false;

            while (!done) {
                int eventType = parser.next();

                if (eventType == XmlPullParser.START_TAG)
                {
                    if ("key".equals(parser.getName()))
                        in_key = true;
                    else if ("print".equals(parser.getName()))
                        in_print = true;
                }
                else if (eventType == XmlPullParser.END_TAG)
                {
                    if ("key".equals(parser.getName()))
                        in_key = false;
                    else if ("print".equals(parser.getName()))
                        in_print = false;
                    else if (ELEMENT_NAME.equals(parser.getName()))
                        done = true;
                }
                else if (eventType == XmlPullParser.TEXT) {
                    if (in_key)
                        key = parser.getText();
                    else if (in_print)
                        print = parser.getText();
                }
            }

            return new PublicKeyPresence(key, print);
        }
    }

}