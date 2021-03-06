package com.aniakanl.http2;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import com.aniakanl.client.ExBufferedInputStream;
import com.aniakanl.http2.frame.BaseFrame;
import com.aniakanl.http2.frame.FrameSerializer;
import com.aniakanl.http2.frame.FrameType;
import com.aniakanl.http2.frame.SettingIdentifier;
import com.aniakanl.http2.frame.SettingParameter;
import com.aniakanl.http2.frame.SettingsFrame;

public class HTTP2Connection {

	static final String PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";

	private ExBufferedInputStream inputStream;

	private BufferedOutputStream outputStream;

	private int lastSeenStreamId = 0;

	private HashMap<Integer, HTTP2Stream> http2Streams = new HashMap<>();

	private HashMap<SettingIdentifier, SettingParameter> remoteSettings = new HashMap<>();

	private HashMap<SettingIdentifier, SettingParameter> localSettings = new HashMap<>();

	/**
	 * Constructor to instantiate HTTP2Connection object
	 * 
	 * @param input
	 *            HTTP2Client passes the ExBufferedInputStream
	 * @param output
	 */
	public HTTP2Connection(ExBufferedInputStream input, BufferedOutputStream output) {

		this.inputStream = input;
		this.outputStream = output;
	}

	public HashMap<SettingIdentifier, SettingParameter> getRemoteSettings() {
		return remoteSettings;
	}

	public void setRemoteSettings(HashMap<SettingIdentifier, SettingParameter> remoteSettings) {
		this.remoteSettings = remoteSettings;
	}

	public HashMap<SettingIdentifier, SettingParameter> getLocalSettings() {
		return localSettings;
	}

	public void setLocalSettings(HashMap<SettingIdentifier, SettingParameter> localSettings) {
		this.localSettings = localSettings;
	}

	/**
	 * Function to validate the PREFACE received on the input stream from the
	 * remote system
	 * 
	 * @return true if preface is valid
	 * @throws IOException
	 */
	public boolean hasProperPreface() throws IOException {
		boolean result = false;

		byte[] preface = inputStream.readFully(24);

		String prefaceStr = new String(preface, 0, preface.length);

		if (prefaceStr.equals(PREFACE)) {
			result = true;
		}

		return result;
	}

	/**
	 * [rfc7540 Section 5.1.1] Streams are identified with an unsigned 31-bit
	 * integer. Streams initiated by a client MUST use odd-numbered stream
	 * identifiers; those initiated by the server MUST use even-numbered stream
	 * identifiers. A stream identifier of zero (0x0) is used for connection
	 * control messages; the stream identifier of zero cannot be used to
	 * establish a new stream.
	 * 
	 * @throws Exception
	 */
	// TODO check for odd and even stream identifiers
	public void handle() throws Exception {
		BaseFrame frame = null;

		// main HTTP2
		while ((frame = FrameSerializer.deserialize(inputStream)) != null) {

			// rfc7540 (section 6.5): SETTINGS frames always apply to a
			// connection, never a single stream.
			if (frame.getHeader().getType() == FrameType.SETTINGS) {
				updateRemoteSettings((SettingsFrame) frame);
				sendAckSettings();
			} else {
				HTTP2Stream targetStream = null;

				int streamId = frame.getHeader().getStreamIdentifier();

				// check if streamID is already present in the hashmap, i.e., a
				// already established stream
				if (http2Streams.containsKey(streamId) == true) {
					targetStream = http2Streams.get(streamId);
				} else if (lastSeenStreamId < streamId) {
					// streamID is not present and has to be greater than all
					// the stream IDs present
					targetStream = new HTTP2Stream(streamId);
					http2Streams.put(streamId, targetStream);
					lastSeenStreamId = streamId;
				}
				// TODO add else error code
				if (targetStream != null) {
					targetStream.processFrame(frame);
				}
			}
		}

	}

	public void updateRemoteSettings(SettingsFrame remoteSettingFrame) {

		/**
		 * [rfc7540 section 6.5] The stream identifier for a SETTINGS frame MUST
		 * be zero (0x0). If an endpoint receives a SETTINGS frame whose stream
		 * identifier field is anything other than 0x0, the endpoint MUST
		 * respond with a connection error (Section 5.4.1) of type
		 * PROTOCOL_ERROR.
		 */

		if (remoteSettingFrame.getHeader().getStreamIdentifier() == 0) {
			for (SettingParameter parameter : remoteSettingFrame.getSettingParameters()) {
				getRemoteSettings().put(parameter.identifier, parameter);
			}
		} else {
			// TODO respond with a connection error
		}

	}

	public void sendAckSettings() throws IOException {
		SettingsFrame frame = new SettingsFrame();
		byte[] buffer = frame.convertToBinary();

		outputStream.write(buffer, 0, buffer.length);
		outputStream.flush();
	}
}
