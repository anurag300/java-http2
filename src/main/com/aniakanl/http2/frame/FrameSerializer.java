package com.aniakanl.http2.frame;

import com.aniakanl.client.ExBufferedInputStream;

public class FrameSerializer {

	public static BaseFrame deserialize(ExBufferedInputStream bufferedInputStream) throws Exception {

		BaseFrame baseFrame = null;
		byte[] tmpBuffer = new byte[9];

		tmpBuffer = bufferedInputStream.readFully(9);

		FrameHeader frameHeader = FrameHeader.Parse(tmpBuffer);
		
		byte[] body = bufferedInputStream.readFully(frameHeader.getLength());
			

		switch (frameHeader.getType()) {
		case HEADERS:
			baseFrame = HeadersFrame.parse(body, frameHeader);
			break;
		case CONTINUATION:
			//baseFrame = new ContinuationFrame();
			break;
		case DATA:
			//baseFrame = new DataFrame();
			break;
		case GOAWAY:
			//baseFrame = new GoawayFrame();
			break;
		case PING:
			//baseFrame = new PingFrame();
			break;
		case PRIORITY:
			//baseFrame = new PriorityFrame();
			break;
		case PUSH_PROMISE:
			//baseFrame = new PushPromiseFrame();
			break;
		case RST_STREAM:
			//baseFrame = new RSTStreamFrame();
			break;
		case SETTINGS:
			baseFrame = SettingsFrame.parse(body, frameHeader);
			break;
		case WINDOW_UPDATE:
			baseFrame = WindowUpdateFrame.parse(body, frameHeader);
			break;
		default:
			break;

		}

		return baseFrame;
	}
	
	public static byte[] serialize(BaseFrame frame)
	{
		return frame.convertToBinary();
	}

}
