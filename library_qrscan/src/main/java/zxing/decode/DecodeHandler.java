/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zxing.decode;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.lyhh.library_qrscan.R;

import java.util.Map;

import listener.IZXingActivity;

public class DecodeHandler extends Handler {
    private boolean running = true;
    private IZXingActivity ivew;
    private DecodeCore decodeCore;

    public DecodeHandler(IZXingActivity ivew, Map<DecodeHintType, Object> hints) {
        decodeCore = new DecodeCore(hints);
        this.ivew = ivew;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);

        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }

    private void decode(byte[] data, int width, int height) {
        Result result = decodeCore.decode(data, width, height, ivew.getCropRect(), true);
        Handler handler = ivew.getHandler();
        if (result != null) {
            // Don't log the barcode contents for security.
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, result);
                message.sendToTarget();
            }
        } else {
            if (handler != null) {
                Message message = Message.obtain(handler, R.id.decode_failed);
                message.sendToTarget();
            }
        }
    }
}
