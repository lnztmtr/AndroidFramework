/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.media;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.SystemWriteManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.net.Proxy;
import android.net.ProxyProperties;
import android.net.Uri;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaTimeProvider;
import android.media.MediaTimeProvider.OnMediaTimeListener;
import android.media.SubtitleController;
import android.media.SubtitleData;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.FileOutputStream;
import java.io.OutputStream;

import java.lang.Runnable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.lang.ref.WeakReference;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.os.RemoteException;
import android.content.ServiceConnection;
import com.amlogic.SubTitleService.ISubTitleService;
import com.cmcc.media.MediaPlayerEx;

// huawei plugin start
import com.huawei.media.aidl.IAnalyticService;
import com.huawei.media.aidl.IAnalyticCallback;
import com.huawei.media.aidl.MAParam;
import android.os.RemoteException;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
// huawei plugin end

import android.content.ComponentName;
import android.os.IBinder;
import android.database.Cursor;
import android.provider.MediaStore;
import java.lang.Integer;
import java.lang.Thread;
import android.os.ServiceManager;
import android.app.SystemWriteManager;
import android.provider.Settings;
import java.util.Random;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import android.text.TextUtils;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.UnknownHostException;
import android.graphics.Bitmap;
import static android.Manifest.permission.RECEIVE_DATA_ACTIVITY_CHANGE;
/**
 * MediaPlayer class can be used to control playback
 * of audio/video files and streams. An example on how to use the methods in
 * this class can be found in {@link android.widget.VideoView}.
 *
 * <p>Topics covered here are:
 * <ol>
 * <li><a href="#StateDiagram">State Diagram</a>
 * <li><a href="#Valid_and_Invalid_States">Valid and Invalid States</a>
 * <li><a href="#Permissions">Permissions</a>
 * <li><a href="#Callbacks">Register informational and error callbacks</a>
 * </ol>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about how to use MediaPlayer, read the
 * <a href="{@docRoot}guide/topics/media/mediaplayer.html">Media Playback</a> developer guide.</p>
 * </div>
 *
 * <a name="StateDiagram"></a>
 * <h3>State Diagram</h3>
 *
 * <p>Playback control of audio/video files and streams is managed as a state
 * machine. The following diagram shows the life cycle and the states of a
 * MediaPlayer object driven by the supported playback control operations.
 * The ovals represent the states a MediaPlayer object may reside
 * in. The arcs represent the playback control operations that drive the object
 * state transition. There are two types of arcs. The arcs with a single arrow
 * head represent synchronous method calls, while those with
 * a double arrow head represent asynchronous method calls.</p>
 *
 * <p><img src="../../../images/mediaplayer_state_diagram.gif"
 *         alt="MediaPlayer State diagram"
 *         border="0" /></p>
 *
 * <p>From this state diagram, one can see that a MediaPlayer object has the
 *    following states:</p>
 * <ul>
 *     <li>When a MediaPlayer object is just created using <code>new</code> or
 *         after {@link #reset()} is called, it is in the <em>Idle</em> state; and after
 *         {@link #release()} is called, it is in the <em>End</em> state. Between these
 *         two states is the life cycle of the MediaPlayer object.
 *         <ul>
 *         <li>There is a subtle but important difference between a newly constructed
 *         MediaPlayer object and the MediaPlayer object after {@link #reset()}
 *         is called. It is a programming error to invoke methods such
 *         as {@link #getCurrentPosition()},
 *         {@link #getDuration()}, {@link #getVideoHeight()},
 *         {@link #getVideoWidth()}, {@link #setAudioStreamType(int)},
 *         {@link #setLooping(boolean)},
 *         {@link #setVolume(float, float)}, {@link #pause()}, {@link #start()},
 *         {@link #stop()}, {@link #seekTo(int)}, {@link #prepare()} or
 *         {@link #prepareAsync()} in the <em>Idle</em> state for both cases. If any of these
 *         methods is called right after a MediaPlayer object is constructed,
 *         the user supplied callback method OnErrorListener.onError() won't be
 *         called by the internal player engine and the object state remains
 *         unchanged; but if these methods are called right after {@link #reset()},
 *         the user supplied callback method OnErrorListener.onError() will be
 *         invoked by the internal player engine and the object will be
 *         transfered to the <em>Error</em> state. </li>
 *         <li>It is also recommended that once
 *         a MediaPlayer object is no longer being used, call {@link #release()} immediately
 *         so that resources used by the internal player engine associated with the
 *         MediaPlayer object can be released immediately. Resource may include
 *         singleton resources such as hardware acceleration components and
 *         failure to call {@link #release()} may cause subsequent instances of
 *         MediaPlayer objects to fallback to software implementations or fail
 *         altogether. Once the MediaPlayer
 *         object is in the <em>End</em> state, it can no longer be used and
 *         there is no way to bring it back to any other state. </li>
 *         <li>Furthermore,
 *         the MediaPlayer objects created using <code>new</code> is in the
 *         <em>Idle</em> state, while those created with one
 *         of the overloaded convenient <code>create</code> methods are <em>NOT</em>
 *         in the <em>Idle</em> state. In fact, the objects are in the <em>Prepared</em>
 *         state if the creation using <code>create</code> method is successful.
 *         </li>
 *         </ul>
 *         </li>
 *     <li>In general, some playback control operation may fail due to various
 *         reasons, such as unsupported audio/video format, poorly interleaved
 *         audio/video, resolution too high, streaming timeout, and the like.
 *         Thus, error reporting and recovery is an important concern under
 *         these circumstances. Sometimes, due to programming errors, invoking a playback
 *         control operation in an invalid state may also occur. Under all these
 *         error conditions, the internal player engine invokes a user supplied
 *         OnErrorListener.onError() method if an OnErrorListener has been
 *         registered beforehand via
 *         {@link #setOnErrorListener(android.media.MediaPlayer.OnErrorListener)}.
 *         <ul>
 *         <li>It is important to note that once an error occurs, the
 *         MediaPlayer object enters the <em>Error</em> state (except as noted
 *         above), even if an error listener has not been registered by the application.</li>
 *         <li>In order to reuse a MediaPlayer object that is in the <em>
 *         Error</em> state and recover from the error,
 *         {@link #reset()} can be called to restore the object to its <em>Idle</em>
 *         state.</li>
 *         <li>It is good programming practice to have your application
 *         register a OnErrorListener to look out for error notifications from
 *         the internal player engine.</li>
 *         <li>IllegalStateException is
 *         thrown to prevent programming errors such as calling {@link #prepare()},
 *         {@link #prepareAsync()}, or one of the overloaded <code>setDataSource
 *         </code> methods in an invalid state. </li>
 *         </ul>
 *         </li>
 *     <li>Calling
 *         {@link #setDataSource(FileDescriptor)}, or
 *         {@link #setDataSource(String)}, or
 *         {@link #setDataSource(Context, Uri)}, or
 *         {@link #setDataSource(FileDescriptor, long, long)} transfers a
 *         MediaPlayer object in the <em>Idle</em> state to the
 *         <em>Initialized</em> state.
 *         <ul>
 *         <li>An IllegalStateException is thrown if
 *         setDataSource() is called in any other state.</li>
 *         <li>It is good programming
 *         practice to always look out for <code>IllegalArgumentException</code>
 *         and <code>IOException</code> that may be thrown from the overloaded
 *         <code>setDataSource</code> methods.</li>
 *         </ul>
 *         </li>
 *     <li>A MediaPlayer object must first enter the <em>Prepared</em> state
 *         before playback can be started.
 *         <ul>
 *         <li>There are two ways (synchronous vs.
 *         asynchronous) that the <em>Prepared</em> state can be reached:
 *         either a call to {@link #prepare()} (synchronous) which
 *         transfers the object to the <em>Prepared</em> state once the method call
 *         returns, or a call to {@link #prepareAsync()} (asynchronous) which
 *         first transfers the object to the <em>Preparing</em> state after the
 *         call returns (which occurs almost right way) while the internal
 *         player engine continues working on the rest of preparation work
 *         until the preparation work completes. When the preparation completes or when {@link #prepare()} call returns,
 *         the internal player engine then calls a user supplied callback method,
 *         onPrepared() of the OnPreparedListener interface, if an
 *         OnPreparedListener is registered beforehand via {@link
 *         #setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener)}.</li>
 *         <li>It is important to note that
 *         the <em>Preparing</em> state is a transient state, and the behavior
 *         of calling any method with side effect while a MediaPlayer object is
 *         in the <em>Preparing</em> state is undefined.</li>
 *         <li>An IllegalStateException is
 *         thrown if {@link #prepare()} or {@link #prepareAsync()} is called in
 *         any other state.</li>
 *         <li>While in the <em>Prepared</em> state, properties
 *         such as audio/sound volume, screenOnWhilePlaying, looping can be
 *         adjusted by invoking the corresponding set methods.</li>
 *         </ul>
 *         </li>
 *     <li>To start the playback, {@link #start()} must be called. After
 *         {@link #start()} returns successfully, the MediaPlayer object is in the
 *         <em>Started</em> state. {@link #isPlaying()} can be called to test
 *         whether the MediaPlayer object is in the <em>Started</em> state.
 *         <ul>
 *         <li>While in the <em>Started</em> state, the internal player engine calls
 *         a user supplied OnBufferingUpdateListener.onBufferingUpdate() callback
 *         method if a OnBufferingUpdateListener has been registered beforehand
 *         via {@link #setOnBufferingUpdateListener(OnBufferingUpdateListener)}.
 *         This callback allows applications to keep track of the buffering status
 *         while streaming audio/video.</li>
 *         <li>Calling {@link #start()} has not effect
 *         on a MediaPlayer object that is already in the <em>Started</em> state.</li>
 *         </ul>
 *         </li>
 *     <li>Playback can be paused and stopped, and the current playback position
 *         can be adjusted. Playback can be paused via {@link #pause()}. When the call to
 *         {@link #pause()} returns, the MediaPlayer object enters the
 *         <em>Paused</em> state. Note that the transition from the <em>Started</em>
 *         state to the <em>Paused</em> state and vice versa happens
 *         asynchronously in the player engine. It may take some time before
 *         the state is updated in calls to {@link #isPlaying()}, and it can be
 *         a number of seconds in the case of streamed content.
 *         <ul>
 *         <li>Calling {@link #start()} to resume playback for a paused
 *         MediaPlayer object, and the resumed playback
 *         position is the same as where it was paused. When the call to
 *         {@link #start()} returns, the paused MediaPlayer object goes back to
 *         the <em>Started</em> state.</li>
 *         <li>Calling {@link #pause()} has no effect on
 *         a MediaPlayer object that is already in the <em>Paused</em> state.</li>
 *         </ul>
 *         </li>
 *     <li>Calling  {@link #stop()} stops playback and causes a
 *         MediaPlayer in the <em>Started</em>, <em>Paused</em>, <em>Prepared
 *         </em> or <em>PlaybackCompleted</em> state to enter the
 *         <em>Stopped</em> state.
 *         <ul>
 *         <li>Once in the <em>Stopped</em> state, playback cannot be started
 *         until {@link #prepare()} or {@link #prepareAsync()} are called to set
 *         the MediaPlayer object to the <em>Prepared</em> state again.</li>
 *         <li>Calling {@link #stop()} has no effect on a MediaPlayer
 *         object that is already in the <em>Stopped</em> state.</li>
 *         </ul>
 *         </li>
 *     <li>The playback position can be adjusted with a call to
 *         {@link #seekTo(int)}.
 *         <ul>
 *         <li>Although the asynchronuous {@link #seekTo(int)}
 *         call returns right way, the actual seek operation may take a while to
 *         finish, especially for audio/video being streamed. When the actual
 *         seek operation completes, the internal player engine calls a user
 *         supplied OnSeekComplete.onSeekComplete() if an OnSeekCompleteListener
 *         has been registered beforehand via
 *         {@link #setOnSeekCompleteListener(OnSeekCompleteListener)}.</li>
 *         <li>Please
 *         note that {@link #seekTo(int)} can also be called in the other states,
 *         such as <em>Prepared</em>, <em>Paused</em> and <em>PlaybackCompleted
 *         </em> state.</li>
 *         <li>Furthermore, the actual current playback position
 *         can be retrieved with a call to {@link #getCurrentPosition()}, which
 *         is helpful for applications such as a Music player that need to keep
 *         track of the playback progress.</li>
 *         </ul>
 *         </li>
 *     <li>When the playback reaches the end of stream, the playback completes.
 *         <ul>
 *         <li>If the looping mode was being set to <var>true</var>with
 *         {@link #setLooping(boolean)}, the MediaPlayer object shall remain in
 *         the <em>Started</em> state.</li>
 *         <li>If the looping mode was set to <var>false
 *         </var>, the player engine calls a user supplied callback method,
 *         OnCompletion.onCompletion(), if a OnCompletionListener is registered
 *         beforehand via {@link #setOnCompletionListener(OnCompletionListener)}.
 *         The invoke of the callback signals that the object is now in the <em>
 *         PlaybackCompleted</em> state.</li>
 *         <li>While in the <em>PlaybackCompleted</em>
 *         state, calling {@link #start()} can restart the playback from the
 *         beginning of the audio/video source.</li>
 * </ul>
 *
 *
 * <a name="Valid_and_Invalid_States"></a>
 * <h3>Valid and invalid states</h3>
 *
 * <table border="0" cellspacing="0" cellpadding="0">
 * <tr><td>Method Name </p></td>
 *     <td>Valid Sates </p></td>
 *     <td>Invalid States </p></td>
 *     <td>Comments </p></td></tr>
 * <tr><td>attachAuxEffect </p></td>
 *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted} </p></td>
 *     <td>{Idle, Error} </p></td>
 *     <td>This method must be called after setDataSource.
 *     Calling it does not change the object state. </p></td></tr>
 * <tr><td>getAudioSessionId </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>getCurrentPosition </p></td>
 *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
 *         PlaybackCompleted} </p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method in a valid state does not change the
 *         state. Calling this method in an invalid state transfers the object
 *         to the <em>Error</em> state. </p></td></tr>
 * <tr><td>getDuration </p></td>
 *     <td>{Prepared, Started, Paused, Stopped, PlaybackCompleted} </p></td>
 *     <td>{Idle, Initialized, Error} </p></td>
 *     <td>Successful invoke of this method in a valid state does not change the
 *         state. Calling this method in an invalid state transfers the object
 *         to the <em>Error</em> state. </p></td></tr>
 * <tr><td>getVideoHeight </p></td>
 *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
 *         PlaybackCompleted}</p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method in a valid state does not change the
 *         state. Calling this method in an invalid state transfers the object
 *         to the <em>Error</em> state.  </p></td></tr>
 * <tr><td>getVideoWidth </p></td>
 *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
 *         PlaybackCompleted}</p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method in a valid state does not change
 *         the state. Calling this method in an invalid state transfers the
 *         object to the <em>Error</em> state. </p></td></tr>
 * <tr><td>isPlaying </p></td>
 *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
 *          PlaybackCompleted}</p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method in a valid state does not change
 *         the state. Calling this method in an invalid state transfers the
 *         object to the <em>Error</em> state. </p></td></tr>
 * <tr><td>pause </p></td>
 *     <td>{Started, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Prepared, Stopped, Error}</p></td>
 *     <td>Successful invoke of this method in a valid state transfers the
 *         object to the <em>Paused</em> state. Calling this method in an
 *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
 * <tr><td>prepare </p></td>
 *     <td>{Initialized, Stopped} </p></td>
 *     <td>{Idle, Prepared, Started, Paused, PlaybackCompleted, Error} </p></td>
 *     <td>Successful invoke of this method in a valid state transfers the
 *         object to the <em>Prepared</em> state. Calling this method in an
 *         invalid state throws an IllegalStateException.</p></td></tr>
 * <tr><td>prepareAsync </p></td>
 *     <td>{Initialized, Stopped} </p></td>
 *     <td>{Idle, Prepared, Started, Paused, PlaybackCompleted, Error} </p></td>
 *     <td>Successful invoke of this method in a valid state transfers the
 *         object to the <em>Preparing</em> state. Calling this method in an
 *         invalid state throws an IllegalStateException.</p></td></tr>
 * <tr><td>release </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>After {@link #release()}, the object is no longer available. </p></td></tr>
 * <tr><td>reset </p></td>
 *     <td>{Idle, Initialized, Prepared, Started, Paused, Stopped,
 *         PlaybackCompleted, Error}</p></td>
 *     <td>{}</p></td>
 *     <td>After {@link #reset()}, the object is like being just created.</p></td></tr>
 * <tr><td>seekTo </p></td>
 *     <td>{Prepared, Started, Paused, PlaybackCompleted} </p></td>
 *     <td>{Idle, Initialized, Stopped, Error}</p></td>
 *     <td>Successful invoke of this method in a valid state does not change
 *         the state. Calling this method in an invalid state transfers the
 *         object to the <em>Error</em> state. </p></td></tr>
 * <tr><td>setAudioSessionId </p></td>
 *     <td>{Idle} </p></td>
 *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted,
 *          Error} </p></td>
 *     <td>This method must be called in idle state as the audio session ID must be known before
 *         calling setDataSource. Calling it does not change the object state. </p></td></tr>
 * <tr><td>setAudioStreamType </p></td>
 *     <td>{Idle, Initialized, Stopped, Prepared, Started, Paused,
 *          PlaybackCompleted}</p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method does not change the state. In order for the
 *         target audio stream type to become effective, this method must be called before
 *         prepare() or prepareAsync().</p></td></tr>
 * <tr><td>setAuxEffectSendLevel </p></td>
 *     <td>any</p></td>
 *     <td>{} </p></td>
 *     <td>Calling this method does not change the object state. </p></td></tr>
 * <tr><td>setDataSource </p></td>
 *     <td>{Idle} </p></td>
 *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted,
 *          Error} </p></td>
 *     <td>Successful invoke of this method in a valid state transfers the
 *         object to the <em>Initialized</em> state. Calling this method in an
 *         invalid state throws an IllegalStateException.</p></td></tr>
 * <tr><td>setDisplay </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setSurface </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setVideoScalingMode </p></td>
 *     <td>{Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted} </p></td>
 *     <td>{Idle, Error}</p></td>
 *     <td>Successful invoke of this method does not change the state.</p></td></tr>
 * <tr><td>setLooping </p></td>
 *     <td>{Idle, Initialized, Stopped, Prepared, Started, Paused,
 *         PlaybackCompleted}</p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method in a valid state does not change
 *         the state. Calling this method in an
 *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
 * <tr><td>isLooping </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setOnBufferingUpdateListener </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setOnCompletionListener </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setOnErrorListener </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setOnPreparedListener </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setOnSeekCompleteListener </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state. </p></td></tr>
 * <tr><td>setScreenOnWhilePlaying</></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state.  </p></td></tr>
 * <tr><td>setVolume </p></td>
 *     <td>{Idle, Initialized, Stopped, Prepared, Started, Paused,
 *          PlaybackCompleted}</p></td>
 *     <td>{Error}</p></td>
 *     <td>Successful invoke of this method does not change the state.
 * <tr><td>setWakeMode </p></td>
 *     <td>any </p></td>
 *     <td>{} </p></td>
 *     <td>This method can be called in any state and calling it does not change
 *         the object state.</p></td></tr>
 * <tr><td>start </p></td>
 *     <td>{Prepared, Started, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Stopped, Error}</p></td>
 *     <td>Successful invoke of this method in a valid state transfers the
 *         object to the <em>Started</em> state. Calling this method in an
 *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
 * <tr><td>stop </p></td>
 *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Error}</p></td>
 *     <td>Successful invoke of this method in a valid state transfers the
 *         object to the <em>Stopped</em> state. Calling this method in an
 *         invalid state transfers the object to the <em>Error</em> state.</p></td></tr>
 * <tr><td>getTrackInfo </p></td>
 *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Error}</p></td>
 *     <td>Successful invoke of this method does not change the state.</p></td></tr>
 * <tr><td>addTimedTextSource </p></td>
 *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Error}</p></td>
 *     <td>Successful invoke of this method does not change the state.</p></td></tr>
 * <tr><td>selectTrack </p></td>
 *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Error}</p></td>
 *     <td>Successful invoke of this method does not change the state.</p></td></tr>
 * <tr><td>deselectTrack </p></td>
 *     <td>{Prepared, Started, Stopped, Paused, PlaybackCompleted}</p></td>
 *     <td>{Idle, Initialized, Error}</p></td>
 *     <td>Successful invoke of this method does not change the state.</p></td></tr>
 *
 * </table>
 *
 * <a name="Permissions"></a>
 * <h3>Permissions</h3>
 * <p>One may need to declare a corresponding WAKE_LOCK permission {@link
 * android.R.styleable#AndroidManifestUsesPermission &lt;uses-permission&gt;}
 * element.
 *
 * <p>This class requires the {@link android.Manifest.permission#INTERNET} permission
 * when used with network-based content.
 *
 * <a name="Callbacks"></a>
 * <h3>Callbacks</h3>
 * <p>Applications may want to register for informational and error
 * events in order to be informed of some internal state update and
 * possible runtime errors during playback or streaming. Registration for
 * these events is done by properly setting the appropriate listeners (via calls
 * to
 * {@link #setOnPreparedListener(OnPreparedListener)}setOnPreparedListener,
 * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}setOnVideoSizeChangedListener,
 * {@link #setOnSeekCompleteListener(OnSeekCompleteListener)}setOnSeekCompleteListener,
 * {@link #setOnCompletionListener(OnCompletionListener)}setOnCompletionListener,
 * {@link #setOnBufferingUpdateListener(OnBufferingUpdateListener)}setOnBufferingUpdateListener,
 * {@link #setOnInfoListener(OnInfoListener)}setOnInfoListener,
 * {@link #setOnErrorListener(OnErrorListener)}setOnErrorListener, etc).
 * In order to receive the respective callback
 * associated with these listeners, applications are required to create
 * MediaPlayer objects on a thread with its own Looper running (main UI
 * thread by default has a Looper running).
 *
 */
public class MediaPlayer implements SubtitleController.Listener, MediaPlayerEx
{
    /**
       Constant to retrieve only the new metadata since the last
       call.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean METADATA_UPDATE_ONLY = true;

    /**
       Constant to retrieve all the metadata.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean METADATA_ALL = false;

    /**
       Constant to enable the metadata filter during retrieval.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean APPLY_METADATA_FILTER = true;

    /**
       Constant to disable the metadata filter during retrieval.
       // FIXME: unhide.
       // FIXME: add link to getMetadata(boolean, boolean)
       {@hide}
     */
    public static final boolean BYPASS_METADATA_FILTER = false;

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    private final static String TAG = "MediaPlayer";
    // Name of the remote interface for the media player. Must be kept
    // in sync with the 2nd parameter of the IMPLEMENT_META_INTERFACE
    // macro invocation in IMediaPlayer.cpp
    private final static String IMEDIA_PLAYER = "android.media.IMediaPlayer";
    private static final int INVOKE_ID_CMD_GET_IP_PORT = 5503;
	
    private int mNativeContext; // accessed by native methods
    private int mNativeSurfaceTexture;  // accessed by native methods
    private int mListenerContext; // accessed by native methods
    private SurfaceHolder mSurfaceHolder;
    private EventHandler mEventHandler;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;
    private Context mContext;
    private Intent cmd = null;
    private Intent cmd1 = null;
    private Intent cmdSeek = null;
    private Intent cmdBuffer = null;
    private Intent it_report_unicom = null;//for unicom soft detector and visualable collector
    private MediaInfo info = null;
    private boolean needosdvideo = false;  //flag from surface

    private static int PLAYER_ID_POOL=0;
    private int lowerPlayerId = -1;

    private final static int PLAYER_ID_POOL_SIZE = 1024;
    private static int playerId = 0;
    private final static String idFilePath = "/tmp/TempId.txt";
    private File idFile = new File(idFilePath);
    private int detectId = 0;

    private String mPath;
    private String mSubPath;
	private MediaPlayer mPlayer;
    private Thread mThread = null;
    private SaveLogThread mSaveLogThread = null;
    private Looper mSavelogLooper = null;
    private boolean mSendPrepareEvent = false;
    private boolean mSendQuitEvent = false;
    private long mSetDataSourceTime = 0;
    private long mReportTimeOffset = 0;
    private long mLastReportTime = 0;
    private int  mSeekTime = -1;
    private boolean mPauseFlag = false;
    private boolean mBufferStartFlag = false;
   
    private long  mSeekEndTime = -1;
    private long  mBufferStartTime = -1;
    private long  mBufferEndTime = -1;

	//add by ysten-mark for huawei softdetector
    private int mDuration = 0;
    private int mBitRate = 0;
    private int mVideoRate = 0;
    private int mServerPort = 0;
	private String mstrServerIP = "";
	//end by ysten-mark for huawei softdetector
	
    private boolean mSeekNeedResend = false;
    private boolean mSeekStartSend = false;
    private boolean mBufferingStartSend = false;
    ////add by chenfeng at 20191005:fix MEDIA_BLURREDSCREEN_END may not send
    private boolean mBlurredscreenStartSend = false;
    private boolean mUnloadStartSend = false;
    private int mBlurredscreenStartCnt = 0;
    private boolean  mCmccPlayer = false;
    //add by zhanghk at 20190524:for report two PLAY_QUIT problem
    private boolean mQuitFlag=false;
    //add by zhanghk at 20190525:for report BUFFER after seek problem
    private boolean mSeekFlag=false;
    //add by zhanghk at 20190525:for report many PLAY_START problem
    private boolean mPlayStartFlag=false;

    private SystemWriteManager sw = null;
    private final static String SCREEN_MODE_PATH = "/sys/class/video/screen_mode";
    private final static String FULL_SCREEN_MODE = "1";
    private final static String NORMAL_SCREEN_MODE = "0";

	/*switch audio channel*/
    private final static String AUDIO_CHANNEL_NODE = "/sys/class/amaudio/audio_channels_mask";
	private final static String AUDIO_CHANNEL_LEFT = "l";
	private final static String AUDIO_CHANNEL_RIGHT = "r";
	private final static String AUDIO_CHANNEL_STEREO = "s";

	/*save player's status in file*/
	private final Object mSaveLogLock = new Object();

	/*  huawei plugin add by mark  */
	private IAnalyticService service;

	/**
     * Default constructor. Consider using one of the create() methods for
     * synchronously instantiating a MediaPlayer from a Uri or resource.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances may
     * result in an exception.</p>
     */
    public MediaPlayer() {

        Looper looper;
        mSaveLogThread = new SaveLogThread();
        mSaveLogThread.start();
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        mTimeProvider = new TimeProvider(this);
        mOutOfBandSubtitleTracks = new Vector<SubtitleTrack>();
        mOpenSubtitleSources = new Vector<InputStream>();
        mInbandSubtitleTracks = new SubtitleTrack[0];

        /* Native setup requires a weak reference to our object.
         * It's easier to create it here than in C++.
         */
        native_setup(new WeakReference<MediaPlayer>(this));

		String mMesg = new String("create a new player");
		save_log_in_file(PRINT_LOG_CREATE_PLAYER,mMesg);
	        idFile.setReadable(true,false);
	        idFile.setWritable(true,false);
		mQuitFlag=false;
		mPlayStartFlag=false; 
    }


// huawei plugin start
	IAnalyticCallback callback = new IAnalyticCallback.Stub() {
        @Override
        public boolean isPlaying() throws RemoteException {
            Log.d(TAG, "function isPlaying called");
            return MediaPlayer.this.isPlaying();
        }

        @Override
        public boolean isLive() throws RemoteException {
            Log.d(TAG, "function isLive called");
            return false;
        }

        @Override
        public String getViewSize() throws RemoteException {
            Log.d(TAG, "function getViewSize called");
            return MediaPlayer.this.getVideoWidth() + "*" + MediaPlayer.this.getVideoHeight();
        }

        @Override
        public String getVideoSize() throws RemoteException {
            Log.d(TAG, "function getVideoSize called");
            return MediaPlayer.this.getVideoWidth() + "*" + MediaPlayer.this.getVideoHeight();
        }

        @Override
        public double getStreamLength() throws RemoteException {
            Log.d(TAG, "function getStreamLength called");
            return MediaPlayer.this.getDuration();
        }

        @Override
        public double getPlayBackRate() throws RemoteException {
            Log.d(TAG, "function getPlayBackRate called");
            return 0;
        }

        @Override
        public double getFramesPerSecond() throws RemoteException {
            Log.d(TAG, "function getFramesPerSecond called");
            return 0;
        }

        @Override
        public int getDroppedFrames() throws RemoteException {
            Log.d(TAG, "function getDroppedFrames called");
            return 0;
        }

        @Override
        public double getCurrentPosition() throws RemoteException {
            Log.d(TAG, "function getCurrentPosition called");
            return MediaPlayer.this.getCurrentPosition();
        }

        @Override
        public long getBytesDownloaded() throws RemoteException {
            Log.d(TAG, "function getBytesDownloaded called");
            return 0;
        }

        @Override
        public int getBufferPercentage() throws RemoteException {
            Log.d(TAG, "function getBufferPercentage called");
            return 0;
	};
    };

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            service = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder arg1) {
            Log.d(TAG, "onServiceConnected");
            service = IAnalyticService.Stub.asInterface(arg1);
            if (service == null)
                return;

            try {
				MAParam param = new MAParam();
                param.addParam("DeviceType", "Android STB");
                //begin by zhangyong at 20181121:add neimeng sqm reg 
				if(SystemProperties.get("ro.ysten.province","master").equals("cm201_neimeng")){
					Log.d("zhangyong", "write ntp ip 111.56.116.138 to database");
					param.addParam("RegisterAddress", "TCP://127.0.0.1:37001");//"TCP://183.207.248.21:37000");
                }
				//add by ysten-mark for heilongjiang sqm reg
				else if (SystemProperties.get("ro.ysten.province", "master").contains("heilongjiang")){
					param.addParam("RegisterAddress", "TCP://111.40.205.16:37000");
				}//end add by ysten-mark for heilongjiang sqm reg
				else{
					param.addParam("RegisterAddress", "TCP://111.40.205.16:37000");//"TCP://183.207.248.21:37000");
	        }
                //end by zhangyong at 20181121:add neimeng sqm reg 
                param.addParam("PlayerName", "hiplayer");
                param.addParam("PlayerVersion", "2.1");
                param.addParam("ViewerID", "");
                param.addParam("STBID",SystemProperties.get("ro.serialno", "0000000000000000000000000000000") );
                param.addParam("MACAddress", SystemProperties.get("persist.sys.mac.value", "00:00:00:00:00:00").replace(":", "-"));
                Log.d(TAG, "param:" + param.toString());
                Log.d("zzl", "param:" + param.toString());

	        service.initialize(param);
            } catch (RemoteException e) {
                 e.printStackTrace();
            }
        }
    };

    public void open(String category, String title, String url) {
        Log.d(TAG, "open category:" + category + ", title:" + title + ", url:" + url + ", service:" + service);
        if (service == null)
            return;
        try {
        Log.d(TAG, "open category2:" + category + ", title:" + title + ", url:" + url + ", service:" + service);
            service.open(callback, category, title, url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onEvent(String eventName, String params) {
        Log.d(TAG, "onEvent eventName:" + eventName + ", params:" + params);
        if (service == null)
            return;
        try {
            MAParam maParam = new MAParam();
            if (params != null) {
                maParam = getMAParam(params);
            }
            Log.d(TAG, "onEvent eventName2:" + eventName + ", params:" + maParam.toString());
            service.onEvent(eventName, maParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MAParam getMAParam(String params) {
        String s[] = params.split("&");
        int length = s.length;
        MAParam maParam = new MAParam();
        for (int i = 0; i < length; i++) {
            Log.d(TAG, " param =: " + s[i]);
            String keyAndValue[] = s[i].split("=");
            if (keyAndValue.length != 2)
                continue;
            maParam.addParam(keyAndValue[0], keyAndValue[1]);
        }
        return maParam;
    }

    public void close() {
        Log.d(TAG, "close");
        if (service == null)
            return;
        try {
            service.close();	
            ActivityThread.currentApplication().unbindService(serviceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	//add by ysten-mark for heilongjiang sqm get url string
	private String getUrlString(String s, String s1)
	{
		int postion = s.indexOf(s1);
		int length = s1.length();
		int Length = s.length();
		String newString = s.substring(0, postion) + s.substring(postion + length, Length);
		return newString;
	}
	private String getUrlString1(String url)
	{
		String str_url = "http://";
		if (url.startsWith(str_url))
		{
			url = getUrlString(url, str_url);
			Log.d("mark", "str_url " + url);
		}
		return url;
	}
	//end add by ysten-mark for heilongjiang sqm get url string
    
	private String getCategory(String playUrl){
        if (playUrl.lastIndexOf(".m3u8") >= 0) {
            if (playUrl.indexOf("live") >= 0) {
                 return "Live";
            }
        }
        return "VOD";
    }

	//add by ysten-mark for heilongjiang sqm
   private boolean isIP(String addr) {
    	if(addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {         
    		return false;       
    	}     
    	String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";       
    	Pattern pat = Pattern.compile(rexp);       
    	Matcher mat = pat.matcher(addr);      
        boolean ipAddress = mat.find();      
        if (ipAddress==true){         
			String ips[] = addr.split("\\.");         
        	if(ips.length==4){             
				try{               
					for(String ip : ips){                 
						if(Integer.parseInt(ip)<0||Integer.parseInt(ip)>255){                   
							return false;                 
						}              
        			}            
        		}catch (Exception e){              
        			return false;            
        		}          
        		return true;         
        	}else{         
        		return false;       
        	}  
        }       
        return ipAddress;    
    }
	//end add by ysten-mark for heilongjiang sqm
/*
   private void getIPPort(){

        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();

        try {
            int res = 0;
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(INVOKE_ID_CMD_GET_IP_PORT);
            mPlayer.invoke(request, reply);
            res = reply.readInt();
            if(0 == res){
                mstrServerIP = reply.readString();
                mServerPort = reply.readInt();
            }else{
                mstrServerIP = " ";
                mServerPort = 0;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            request.recycle();
            reply.recycle();
        }

        return;
    }
*/


// huawei plugin end


    private void sendBroadCastPrivate(Intent intent) {
       Log.v(TAG,"sendBroadCastPrivate");
        if (null != mContext)
            mContext.sendBroadcast(intent);
        else
            ActivityManagerNative.broadcastStickyIntent(intent, null, UserHandle.USER_OWNER);
    }

    /*
     * Update the MediaPlayer SurfaceTexture.
     * Call after setting a new display surface.
     */
    private native void _setVideoSurface(Surface surface);

    /* Do not change these values (starting with INVOKE_ID) without updating
     * their counterparts in include/media/mediaplayer.h!
     */
    private static final int INVOKE_ID_GET_TRACK_INFO = 1;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE = 2;
    private static final int INVOKE_ID_ADD_EXTERNAL_SOURCE_FD = 3;
    private static final int INVOKE_ID_SELECT_TRACK = 4;
    private static final int INVOKE_ID_DESELECT_TRACK = 5;
    private static final int INVOKE_ID_SET_VIDEO_SCALE_MODE = 6;
    private static final int INVOKE_ID_NETWORK_SET_PLAYBACK_SPEED = 13;
    private static final int INVOKE_ID_NETWORK_GET_PLAYBACK_SPEED = 14;

    /**
     * Create a request parcel which can be routed to the native media
     * player using {@link #invoke(Parcel, Parcel)}. The Parcel
     * returned has the proper InterfaceToken set. The caller should
     * not overwrite that token, i.e it can only append data to the
     * Parcel.
     *
     * @return A parcel suitable to hold a request for the native
     * player.
     * {@hide}
     */
    public Parcel newRequest() {
        Parcel parcel = Parcel.obtain();
        parcel.writeInterfaceToken(IMEDIA_PLAYER);
        return parcel;
    }

    /**
     * Invoke a generic method on the native player using opaque
     * parcels for the request and reply. Both payloads' format is a
     * convention between the java caller and the native player.
     * Must be called after setDataSource to make sure a native player
     * exists. On failure, a RuntimeException is thrown.
     *
     * @param request Parcel with the data for the extension. The
     * caller must use {@link #newRequest()} to get one.
     *
     * @param reply Output parcel with the data returned by the
     * native player.
     * {@hide}
     */
    public void invoke(Parcel request, Parcel reply) {
        int retcode = native_invoke(request, reply);
        reply.setDataPosition(0);
        if (retcode != 0) {
            throw new RuntimeException("failure code: " + retcode);
        }
    }

    /**
     * Sets the {@link SurfaceHolder} to use for displaying the video
     * portion of the media.
     *
     * Either a surface holder or surface must be set if a display or video sink
     * is needed.  Not calling this method or {@link #setSurface(Surface)}
     * when playing back a video will result in only the audio track being played.
     * A null surface holder or surface will result in only the audio track being
     * played.
     *
     * @param sh the SurfaceHolder to use for video display
     */
    public void setDisplay(SurfaceHolder sh) {
		if (sh != null) {
		// huawei plugin start
		if("A20_neimeng".equals(SystemProperties.get("ro.ysten.province"))
		 ||"heilongjiang".contains(SystemProperties.get("ro,ysten.province"))){     //add by ysten-mark for heilongjiang sqm bindservice
            Log.d(TAG, "bindService start");
            if( service == null) {
                Log.d(TAG, "bindService in");
                ActivityThread.currentApplication().bindService(new Intent("com.huawei.mediaService.MediaAnalyticService"),serviceConnection, Context.BIND_AUTO_CREATE);
            }
            Log.d(TAG, "bindService end");
			}
		//huawei plugin end
		}
		if(sh !=null && sh.getSurface().isValid()){
        mSurfaceHolder = sh;
        Surface surface;
        if (sh != null) {
            surface = sh.getSurface();
        } else {
            surface = null;
        }
        if(surface != null && surface.osd_video_flag)
            needosdvideo = surface.osd_video_flag;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
        if (sh != null){
            mContext = sh.getSurfaceContext();
            Log.d(TAG, "get Context : " + mContext);
            if(mContext != null){
                final ContentResolver resolver = mContext.getContentResolver();
                if(sw == null)
                    sw = (SystemWriteManager) mContext.getSystemService("system_write");

                final String screen_ratio = Settings.Secure.getString(
                        resolver, Settings.Secure.DEFAULT_SCREEN_RATIO);
                if(screen_ratio != null) {
                    if(screen_ratio.equals(FULL_SCREEN_MODE)) {
                        sw.writeSysfs(SCREEN_MODE_PATH, FULL_SCREEN_MODE);
                    }else {
                        sw.writeSysfs(SCREEN_MODE_PATH, NORMAL_SCREEN_MODE);
                    }
                }else {
                    sw.writeSysfs(SCREEN_MODE_PATH, FULL_SCREEN_MODE);
                }
            }
        }
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addDataScheme("file");
        if ("1".equals(SystemProperties.get("media.cmccplayer.enable")) ||
		     "true".equals(SystemProperties.get("media.cmccplayer.enable"))) {
            mCmccPlayer = true;
            cmd = new Intent("MEDIA_PLAY_UNUSED");
        }else{
            cmd = new Intent("MEDIA_PLAY_MONITOR_MESSAGE");
        }

        if (SystemProperties.getBoolean("media.report.unicom", false)) {
            it_report_unicom = new Intent("MEDIA_PLAY_MONITOR_MESSAGE_UNICOM");
        }
/*
		// huawei plugin start
		if("A20_neimeng".equals(SystemProperties.get("ro.ysten.province"))
		 ||"CM201_heilongjiang".equals(SystemProperties.get("ro,ysten.province"))){
            Log.d(TAG, "bindService start");
            if( service == null) {
                Log.d(TAG, "bindService in");
                ActivityThread.currentApplication().bindService(new Intent(
                       "com.huawei.mediaService.MediaAnalyticService"),
                        serviceConnection, Context.BIND_AUTO_CREATE);
            }
            Log.d(TAG, "bindService end");
			}
		//huawei plugin end

*/

        /*if(mContext !=null){
            mContext.registerReceiver(mMountReceiver, intentFilter);
			}*/
		}
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of
     * the media. This is similar to {@link #setDisplay(SurfaceHolder)}, but
     * does not support {@link #setScreenOnWhilePlaying(boolean)}.  Setting a
     * Surface will un-set any Surface or SurfaceHolder that was previously set.
     * A null surface will result in only the audio track being played.
     *
     * If the Surface sends frames to a {@link SurfaceTexture}, the timestamps
     * returned from {@link SurfaceTexture#getTimestamp()} will have an
     * unspecified zero point.  These timestamps cannot be directly compared
     * between different media sources, different instances of the same media
     * source, or multiple runs of the same program.  The timestamp is normally
     * monotonically increasing and is unaffected by time-of-day adjustments,
     * but it is reset when the position is set.
     *
     * @param surface The {@link Surface} to be used for the video portion of
     * the media.
     */
    public void setSurface(Surface surface) {
        if (mScreenOnWhilePlaying && surface != null) {
            Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective for Surface");
        }
        mSurfaceHolder = null;
        if(surface != null && surface.osd_video_flag)
            needosdvideo = surface.osd_video_flag;
        _setVideoSurface(surface);
        updateSurfaceScreenOn();
    }

    /* Do not change these video scaling mode values below without updating
     * their counterparts in system/window.h! Please do not forget to update
     * {@link #isVideoScalingModeSupported} when new video scaling modes
     * are added.
     */
    /**
     * Specifies a video scaling mode. The content is stretched to the
     * surface rendering area. When the surface has the same aspect ratio
     * as the content, the aspect ratio of the content is maintained;
     * otherwise, the aspect ratio of the content is not maintained when video
     * is being rendered. Unlike {@link #VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING},
     * there is no content cropping with this video scaling mode.
     */
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;

    /**
     * Specifies a video scaling mode. The content is scaled, maintaining
     * its aspect ratio. The whole surface area is always used. When the
     * aspect ratio of the content is the same as the surface, no content
     * is cropped; otherwise, content is cropped to fit the surface.
     */
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    /**
     * Sets video scaling mode. To make the target video scaling mode
     * effective during playback, this method must be called after
     * data source is set. If not called, the default video
     * scaling mode is {@link #VIDEO_SCALING_MODE_SCALE_TO_FIT}.
     *
     * <p> The supported video scaling modes are:
     * <ul>
     * <li> {@link #VIDEO_SCALING_MODE_SCALE_TO_FIT}
     * <li> {@link #VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING}
     * </ul>
     *
     * @param mode target video scaling mode. Most be one of the supported
     * video scaling modes; otherwise, IllegalArgumentException will be thrown.
     *
     * @see MediaPlayer#VIDEO_SCALING_MODE_SCALE_TO_FIT
     * @see MediaPlayer#VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
     */
    public void setVideoScalingMode(int mode) {
        if (!isVideoScalingModeSupported(mode)) {
            final String msg = "Scaling mode " + mode + " is not supported";
            throw new IllegalArgumentException(msg);
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(INVOKE_ID_SET_VIDEO_SCALE_MODE);
            request.writeInt(mode);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }
    public void setSpeed(float speed) {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        Log.w(TAG, "setSpeed:"+ speed);
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(INVOKE_ID_NETWORK_SET_PLAYBACK_SPEED);
            request.writeFloat(speed);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }
    public float getSpeed() {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        float ret = 0;
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(INVOKE_ID_NETWORK_GET_PLAYBACK_SPEED);
            invoke(request, reply);
            ret = reply.readFloat();
        } finally {
            request.recycle();
            reply.recycle();
        }
        return ret;
    }

    /**
     * Convenience method to create a MediaPlayer for a given Uri.
     * On success, {@link #prepare()} will already have been called and must not be called again.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances will
     * result in an exception.</p>
     *
     * @param context the Context to use
     * @param uri the Uri from which to get the datasource
     * @return a MediaPlayer object, or null if creation failed
     */
    public static MediaPlayer create(Context context, Uri uri) {
        return create (context, uri, null);
    }

    /**
     * Convenience method to create a MediaPlayer for a given Uri.
     * On success, {@link #prepare()} will already have been called and must not be called again.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances will
     * result in an exception.</p>
     *
     * @param context the Context to use
     * @param uri the Uri from which to get the datasource
     * @param holder the SurfaceHolder to use for displaying the video
     * @return a MediaPlayer object, or null if creation failed
     */
    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder) {

        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(context, uri);
            if (holder != null) {
                mp.setDisplay(holder);
            }
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }

        return null;
    }

    // Note no convenience method to create a MediaPlayer with SurfaceTexture sink.

    /**
     * Convenience method to create a MediaPlayer for a given resource id.
     * On success, {@link #prepare()} will already have been called and must not be called again.
     * <p>When done with the MediaPlayer, you should call  {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances will
     * result in an exception.</p>
     *
     * @param context the Context to use
     * @param resid the raw resource id (<var>R.raw.&lt;something></var>) for
     *              the resource to use as the datasource
     * @return a MediaPlayer object, or null if creation failed
     */
    public static MediaPlayer create(Context context, int resid) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(resid);
            if (afd == null) return null;

            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
           // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }
        return null;
    }

    /**
     * Sets the data source as a content Uri.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(Context context, Uri uri)
        throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(context, uri, null);
        mSetDataSourceTime = System.currentTimeMillis();
    }

    /**
     * Sets the data source as a content Uri.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @param headers the headers to be sent together with the request for the data
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(Context context, Uri uri, Map<String, String> headers)
        throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        disableProxyListener();
        getSubtitleService();
        mSetDataSourceTime = System.currentTimeMillis();

        String scheme = uri.getScheme();
        if(scheme == null || scheme.equals("file")) {
            //add for subtitle service
            mPath = uri.getPath();
            setDataSource(uri.getPath());
            return;
        }

        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            //add for subtitle service
            String mediaStorePath = uri.getPath();
            String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA
            };

            if(scheme.equals("content")) {
                int idx_check = (uri.toString()).indexOf("media/external/video/media");
                if(idx_check > -1) {
                    int idx = mediaStorePath.lastIndexOf("/");
                    String idStr = mediaStorePath.substring(idx+1);
                    int id = Integer.parseInt(idStr);
                    if(subtitleServiceDebug()) Log.i(TAG,"[setDataSource]id:"+id);
                    String where = MediaStore.Video.Media._ID + "=" + id;
                    Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cols, where , null, null);
                    if (cursor != null && cursor.getCount() == 1) {
                        int colidx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                        cursor.moveToFirst();
                        mPath = cursor.getString(colidx);
                        if(subtitleServiceDebug()) Log.i(TAG,"[setDataSource]mediaStorePath:"+mediaStorePath+",mPath:"+mPath);
                    }
                }
                else {
                    mPath = null;
                }
            }
            else {
                mPath = null;
            }
            fd = resolver.openAssetFileDescriptor(uri, "r");
            if (fd == null) {
                return;
            }
            // Note: using getDeclaredLength so that our behavior is the same
            // as previous versions when the content provider is returning
            // a full file.
            if (fd.getDeclaredLength() < 0) {
                setDataSource(fd.getFileDescriptor());
            } else {
                setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        } catch (SecurityException ex) {
        } catch (IOException ex) {
        } finally {
            if (fd != null) {
                fd.close();
            }
        }

        Log.d(TAG, "Couldn't open file on client side, trying server side");

        setDataSource(uri.toString(), headers);

        if (scheme.equalsIgnoreCase("http")
                || scheme.equalsIgnoreCase("https")) {
            setupProxyListener(context);
        }
    }

    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     * @throws IllegalStateException if it is called in an invalid state
     *
     * <p>When <code>path</code> refers to a local file, the file may actually be opened by a
     * process other than the calling application.  This implies that the pathname
     * should be an absolute path (as any other process runs with unspecified current working
     * directory), and that the pathname should reference a world-readable file.
     * As an alternative, the application could first open the file for reading,
     * and then use the file descriptor form {@link #setDataSource(FileDescriptor)}.
     */
    public void setDataSource(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        setDataSource(path, null, null);
        mSetDataSourceTime = System.currentTimeMillis();
    }

    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     * @param headers the headers associated with the http request for the stream you want to play
     * @throws IllegalStateException if it is called in an invalid state
     * @hide pending API council
     */
    public void setDataSource(String path, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException
    {
        String[] keys = null;
        String[] values = null;

        if (headers != null) {
            keys = new String[headers.size()];
            values = new String[headers.size()];

            int i = 0;
            for (Map.Entry<String, String> entry: headers.entrySet()) {
                keys[i] = entry.getKey();
                values[i] = entry.getValue();
                ++i;
            }
        }
        mSetDataSourceTime = System.currentTimeMillis();
        setDataSource(path, keys, values);
    }

    private void setDataSource(String path, String[] keys, String[] values)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        disableProxyListener();
        getSubtitleService();

        final Uri uri = Uri.parse(path);
        if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        }

        final File file = new File(path);
        mPath = path;
        if (file.exists()) {
	    Log.e(TAG, "dbg:// 1file.exists() playerId"+playerId);	
	    updateId();
		Log.e(TAG, "dbg:// 2file.exists() playerId"+playerId);		
            Message msg = mEventHandler.obtainMessage();
            msg.what = MEDIA_SET_DATASOURCE;
            msg.obj = path;
            mEventHandler.handleMessage(msg);

            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            setDataSource(fd);
            is.close();
        } else {
		    Log.e(TAG, "dbg:// 3!file.exists() playerId"+playerId);	
            updateId();
		    Log.e(TAG, "dbg:// 4!file.exists() playerId"+playerId);				
            mbIsFileDescriptor = false;
            Message msg = mEventHandler.obtainMessage();
            msg.what = MEDIA_SET_DATASOURCE;
            msg.obj = path;
            mEventHandler.handleMessage(msg);
            _setDataSource(path, keys, values);
        }
		//modify by zhaolianghua at 20181109 begin:set drm info for hebei
	if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
		Log.d(TAG,"DRM URL path = "+path);
		//donot use now
		//setEleFromPath(path);
	}
	String mMesg = new String("setDataSource,url:");
	mMesg = mMesg + path ;
	save_log_in_file(PRINT_LOG_SETPLAYER_SOURCE,mMesg);
    }

    //get mediatype&contentID from url path for DRM
    private String str1 = "HBGDMediaType=";
    private String str2 = "HBGDContentID=";
    private void setEleFromPath(String path){
	if(path.contains("HBGD")){
		String mediaType = path.substring(path.indexOf(str1)+str1.length(),path.indexOf(str1)+str1.length()+1);
		String contentId = path.substring(path.indexOf(str2)+str2.length());
		SystemProperties.set("media.libplayer.udrm.mediatype", mediaType);
		SystemProperties.set("media.libplayer.udrm.contentid", contentId);
		Log.d(TAG,"DRM mediaType = "+mediaType+" ; contentId = "+contentId);
	}else{
		SystemProperties.set("media.libplayer.udrm.mediatype", "");
		SystemProperties.set("media.libplayer.udrm.contentid", "");
	}
    }
    public void setDrmAuthInfo(int type, String token, String contentId) {
	String authUrl = "MediaType="+type+"&UserToken="+token+"&ContentID="+contentId;
	SystemProperties.set("media.libplayer.udrm.mediatype",type+"");
	SystemProperties.set("media.libplayer.udrm.usertoken",token);
	SystemProperties.set("media.libplayer.udrm.contentid",contentId);
	Log.e(TAG, "hebei drm authUrl:"+authUrl);
    }

    private native void _setDataSource(
        String path, String[] keys, String[] values)
        throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    /**
     * Sets the data source (FileDescriptor) to use. It is the caller's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        // intentionally less than LONG_MAX
        setDataSource(fd, 0, 0x7ffffffffffffffL);
        mSetDataSourceTime = System.currentTimeMillis();
    }

    /**
     * Sets the data source (FileDescriptor) to use.  The FileDescriptor must be
     * seekable (N.B. a LocalSocket is not seekable). It is the caller's responsibility
     * to close the file descriptor. It is safe to do so as soon as this call returns.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts, in bytes
     * @param length the length in bytes of the data to be played
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        disableProxyListener();
        _setDataSource(fd, offset, length);
        mSetDataSourceTime = System.currentTimeMillis();
        mbIsFileDescriptor = true;
    }

    private native void _setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException;

    //---- add for subtitle service---------------------------------------------------------------------------
    private ISubTitleService subTitleService = null;
    private boolean mThreadStop = false;
    private boolean mSubtitleLoad = false;
    private boolean mSubtitleStarted = false;
    public int subtitleOpen(String path) {
        if(subtitleServiceDisable()) {
            return -1;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleOpen] path:"+path);

        if(path.startsWith("/data/") || path.equals("")) {
            return -1;
        }

        try {
            if(subTitleService != null) {
                mSubtitleLoad = false;
                subTitleService.open(path);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /**
     * @hide
     */
    public void subtitleLoad(String path) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleLoad] path:"+path);

        try {
            if(subTitleService != null) {
                mSubtitleLoad = subTitleService.load(path);
                subtitleStart();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @hide
     */
    public void subtitleOpenIdx(int idx) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleOpenIdx] idx:"+idx);

        if(idx < 0) {
            return;
        }

        try {
            if(subTitleService != null) {
                mSubtitleLoad = false;
                subTitleService.openIdx(idx);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void subtitleStart() {
    	  int dispWidth;
    	  int dispHeight;
    	  String graphics_fb0_window_axis = "/sys/class/graphics/fb0/window_axis";
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleStart] mEventHandler:"+mEventHandler);
        if (sw !=null) { 
        	  String[] a = sw.readSysfs(graphics_fb0_window_axis).split(" ");
            dispWidth = Integer.parseInt(a[2]);
            dispHeight = Integer.parseInt(a[3]);
            Log.d(TAG,"setSurfaceViewParam "+"-dispWidth: "+dispWidth+"-dispHeight: "+dispHeight);
            setSurfaceViewParam(0,0,dispWidth,dispHeight);
        }
        if (mEventHandler != null && !mSubtitleStarted) {
            mThreadStop = false;
            mEventHandler.removeMessages(MEDIA_AML_SUBTITLE_START);
            Message m = mEventHandler.obtainMessage(MEDIA_AML_SUBTITLE_START);
            mEventHandler.sendMessageDelayed(m, 500);
        }
    }
    /**
    * @hide
    */
    public void setSurfaceViewParam(int x, int y, int w, int h) {
        Log.i(TAG,"[setSurfaceViewParam] x:" + x + ", y:" + y + ", w:" + w + ",h:" + h);
        try {
            if (subTitleService != null) {
                subTitleService.setSurfaceViewParam(x, y, w, h);
            }
        } catch (RemoteException e) {
            throw new RuntimeException (e);
        }
    }

    private void subtitleClose() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleClose] ");

        if (mEventHandler != null) {
            mEventHandler.removeMessages(MEDIA_AML_SUBTITLE_START);
        }

        mAVTrackNum = 0;
        mTrackGot = false;

        try {
            if(subTitleService != null ) {
                mSubtitleLoad = false;
                mSubtitleStarted = false;
                subTitleService.close();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        if(mThread != null){
            mThreadStop = true;
            mThread = null;
        }
    }

    private void subtitleShow() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleShow] subtitleTotal:"+subtitleTotal()+", mSubtitleLoad:"+mSubtitleLoad);

        if(subtitleTotal() > 0 || mSubtitleLoad) {
            if(subtitleServiceDebug()) Log.i(TAG,"[subtitleStartShow]mThread:"+mThread);
            if(mThread == null) {
                mSubtitleStarted = true;
                mThread = new Thread(runnable);
                mThread.start();
            }
        }
    }

    /**
    * @hide
    */
    public void subtitleOption() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleOption] ");

        try {
            if(subTitleService != null) {
                subTitleService.option();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public int subtitleTotal() {
        if(subtitleServiceDisable()) {
            return 0;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleTotal] ");

        int ret = 0;
        try {
            if(subTitleService != null) {
                ret = subTitleService.getSubTotal();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleTotal] ret:"+ret);
        return ret;
    }


    /**
    * @hide
    */
    public int subtitleInnerTotal() {
        if(subtitleServiceDisable()) {
            return 0;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleInnerTotal] ");

        int ret = 0;
        try {
            if(subTitleService != null) {
                ret = subTitleService.getInnerSubTotal();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleInnerTotal] ret:"+ret);
        return ret;
    }


    /**
    * @hide
    */
    public int subtitleExternalTotal() {
        if(subtitleServiceDisable()) {
            return 0;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleExternalTotal] ");

        int ret = 0;
        try {
            if(subTitleService != null) {
                ret = subTitleService.getExternalSubTotal();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleExternalTotal] ret:"+ret);
        return ret;
    }

    /**
    * @hide
    */
    public void subtitleNext() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleNext] ");

        try {
            if(subTitleService != null) {
                mSubtitleLoad = false;
                subTitleService.nextSub();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitlePre() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitlePre] ");

        try {
            if(subTitleService != null) {
                mSubtitleLoad = false;
                subTitleService.preSub();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public int subtitleGetSubType() {
        if(subtitleServiceDisable()) {
            return -1;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubType] ");

        int ret = 0;
        try {
            if(subTitleService != null) {
                ret = subTitleService.getSubType();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubType] ret:"+ret);
        return ret;
    }

    /**
    * @hide
    */
    public String subtitleGetSubTypeStr() {
        if(subtitleServiceDisable()) {
            return null;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubTypeStr] ");

        String ret = "";
        try {
            if(subTitleService != null) {
                ret = subTitleService.getSubTypeStr();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubTypeStr] ret:"+ret);
        return ret;
    }

    /**
    * @hide
    */
    public int subtitleGetSubTypeDetial() {
        if(subtitleServiceDisable()) {
            return -1;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubTypeDetial] ");

        int ret = 0;
        try {
            if(subTitleService != null) {
                ret = subTitleService.getSubTypeDetial();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubTypeDetial] ret:"+ret);
        return ret;
    }

    /**
    * @hide
    */
    public int subtitleGetCurSubIdx() {
        if(subtitleServiceDisable()) {
            return -1;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetCurSubIdx] ");

        int ret = 0;
        try {  
            if(subTitleService != null) {
                ret = subTitleService.getCurSubIdx(); 
            }
        } catch (RemoteException e) {  
            throw new RuntimeException(e);  
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetCurSubIdx] ret:"+ret);
        return ret;
    }

    /**
    * @hide
    */
    public void subtitleSetTextColor(int color) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleSetTextColor] color:"+color);

        try {
            if(subTitleService != null) {
                subTitleService.setTextColor(color);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleSetTextSize(int size) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleSetTextSize] size:"+size);

        try {
            if(subTitleService != null) {
                subTitleService.setTextSize(size);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleSetGravity(int gravity) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleSetGravity] gravity:"+gravity);

        try {
            if(subTitleService != null) {
                subTitleService.setGravity(gravity);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleSetTextStyle(int style) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleSetTextStyle] style:"+style);

        try {
            if(subTitleService != null) {
            subTitleService.setTextStyle(style);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleSetPosHeight(int height) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleSetPosHeight] height:"+height);

        try {
            if(subTitleService != null) {
                subTitleService.setPosHeight(height);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleSetImgSubRatio(float ratioW, float ratioH, int maxW, int maxH) {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[setImgSubRatio] ratioW:" + ratioW + ", ratioH:" + ratioH + ",maxW:" + maxW + ",maxH:" + maxH);

        try {
            if(subTitleService != null) {
                subTitleService.setImgSubRatio(ratioW, ratioH, maxW, maxH);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleClear() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleClear]");

        try {
            if(subTitleService != null) {
                subTitleService.clear();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleResetForSeek() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleResetForSeek]");

        try {
            if(subTitleService != null) {
                subTitleService.resetForSeek();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleHide() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleHide]");

        try {
            if(subTitleService != null) {
                subTitleService.hide();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public void subtitleDisplay() {
        if(subtitleServiceDisable()) {
            return;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleDisplay]");

        try {
            if(subTitleService != null) {
                subTitleService.display();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * @hide
    */
    public String subtitleGetCurName() {
        if(subtitleServiceDisable()) {
            return null;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetCurName]");

        String name = null;
        try {
            if(subTitleService != null) {
                name = subTitleService.getCurName();
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetCurName] name:"+name);
        return name;
    }

    private String readSysfs(String path) {
        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return null;
        }

        String str = null;
        StringBuilder value = new StringBuilder();

        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null)
                        value.append(str);
                };
				fr.close();
				br.close();
                if(value != null)
                    return value.toString();
                else
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getCurrentPcr() {
        int pcr = 0;
        long pcrl = 0;
        String str = readSysfs("/sys/class/tsync/pts_pcrscr");
        Log.i(TAG, "[getCurrentPcr]readSysfs str:" + str);
        str = str.substring(2);// skip 0x
        if (str != null) {
            //pcr = (Integer.parseInt(str, 16));//90;// change to ms
            pcrl = (Long.parseLong(str, 16));
            pcr = (int)(pcrl/90);
        }
        Log.i(TAG, "[getCurrentPcr]pcr:" + pcr);
        return pcr;
    }

    /**
    * @hide
    */
    public String subtitleGetSubName(int idx) {
        if(subtitleServiceDisable()) {
            return null;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubName]");

        String name = null;
        try {
            if(subTitleService != null) {
                name = subTitleService.getSubName(idx);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubName] name["+idx+"]:"+name);
        return name;
    }

    /**
    * @hide
    */
    public String subtitleGetSubLanguage(int idx) {
        if(subtitleServiceDisable()) {
            return null;
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubLanguage]");

        String language = null;
        try {
            if(subTitleService != null) {
                language = subTitleService.getSubLanguage(idx);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleGetSubLanguage] language["+idx+"]:"+language);
        return language;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            int pos = 0;
            while(!mThreadStop) {
                if(subtitleServiceDisable()) {
                    mThreadStop = true;
                    break;
                }

                if(isPlaying()) {
                    if(subtitleGetSubTypeDetial() == 6){//6:dvb type
                        pos = getCurrentPcr();
                    }
                    else {
                        pos = getIntParameter(KEY_PARAMETER_AML_PLAYER_GET_REAL_POSITION);
                        if (pos == 0) {
                            pos = getCurrentPosition();
                        }
                    }
                }

                //show subtitle
                if(subtitleServiceDebug()) Log.i(TAG,"[runnable]showSub:"+pos);
                try {
                    if(subTitleService != null) {
                        subTitleService.showSub(pos);
                    }
                    else {
                        mThreadStop = true;
                        break;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Thread.sleep(300 - (pos % 300));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    };

    private boolean subtitleServiceDebug() {
        boolean ret = false;
        if(SystemProperties.getBoolean("sys.mpSubtitleService.debug", false)) {
            ret = true;
        }
        return ret;
    }

    private boolean subtitleServiceDisable() {
        boolean ret = false;
        if(SystemProperties.getBoolean("sys.subtitleService.disable", false)) {
            ret = true;
        }
        return ret;
    }

    private boolean subtitleOptionEnable() {
        boolean ret = false;
        if(SystemProperties.getBoolean("sys.subtitleOption.enable", false)) {
            ret = true;
        }
        return ret;
    }

    private void getSubtitleService() {
        if(subtitleServiceDisable()) {
            return;
        }

        if(subTitleService == null) {
            IBinder b = ServiceManager.getService(Context.SUBTITLE_SERVICE);
            subTitleService = ISubTitleService.Stub.asInterface(b);
        }
        if(subtitleServiceDebug()) Log.i(TAG,"[getSubtitleService] subTitleService:"+subTitleService);
    }

    private void subtitleServiceRelease() {
        if(subtitleServiceDisable()) {
            return;
        }

        if(subtitleServiceDebug()) Log.i(TAG,"[subtitleServiceRelease] ");

        if(subTitleService != null) {
            subTitleService = null;
        }
    }

    /**
     * Prepares the player for playback, synchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For files, it is OK to call prepare(),
     * which blocks until MediaPlayer is ready for playback.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public native void prepare() throws IOException, IllegalStateException;

    /**
     * Prepares the player for playback, asynchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For streams, you should call prepareAsync(),
     * which returns immediately, rather than blocking until enough data has been
     * buffered.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public native void prepareAsync() throws IllegalStateException;

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public  void start() throws IllegalStateException {
        if(SystemProperties.getBoolean("media.player.report.udp", false)&& !mSendPrepareEvent){
            int starttime = getCurrentPosition();
            long time = System.currentTimeMillis()/1000;
            if(null == info)
                info= getMediaInfo();
            StringBuffer buf=new StringBuffer();
            buf.append("<type>"+"Prepare"+"</type>");
            buf.append('\n');
            buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
            buf.append('\n');
            buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
            buf.append('\n');
            buf.append("<url>"+info.url+"</url>");
            sendDataReport(buf.toString(), buf.length());
            mSendPrepareEvent = true;
            mSendQuitEvent = false;

            Log.e(TAG, buf.length()+" ,:"+buf.toString());	
        }else if(mContext != null && !mSendPrepareEvent){
            if(null == info)
                info= getMediaInfo();
            cmd.putExtra("TYPE", "PLAY_PREPARE");
	     mSeekFlag=false;
            cmd.putExtra("START_TIME", mSetDataSourceTime);
            if(null != info){
                cmd.putExtra("ID", playerId);
                Log.d(TAG , "url is : "+info.url);
                cmd.putExtra("URL", info.url);
            }
            if(!mbIsFileDescriptor){
                mContext.sendBroadcast(cmd);
                mSendPrepareEvent = true;
	         Log.e(TAG, "dbg://2448 mSendPrepareEvent:"+mSendPrepareEvent);				
                mSendQuitEvent = false;
                //add by chenfeng at 20191005:fix MEDIA_BLURREDSCREEN_END may not send
                mBufferingStartSend = false;
                mBlurredscreenStartSend = false;
                mUnloadStartSend = false;
                mBlurredscreenStartCnt = 0;

            }
        }

        if(mContext != null && mSeekNeedResend) {
            cmd.putExtra("TYPE", "SEEK_START");
	     mSeekFlag=true;
            cmd.putExtra("PLAY_TIME", mSeekTime/1000);
            cmd.putExtra("START_TIME", System.currentTimeMillis());
            if(null == info)
               info= getMediaInfo();
               cmd.putExtra("ID", playerId);
            if(!mbIsFileDescriptor){
                 if(SystemProperties.get("ro.ysten.province").contains("liaoning") && mBufferingStartSend ){
                      mBufferingStartSend = false;
                      cmdBuffer = cmd;
                      cmdBuffer.putExtra("TYPE", "BUFFER_END");
                      cmdBuffer.putExtra("END_TIME", System.currentTimeMillis());
                      mContext.sendBroadcast(cmdBuffer);
                      int seekStartTime = getCurrentPosition();
                      cmd.putExtra("TYPE","SEEK_START");
                      cmd.putExtra("PLAY_TIME",mSeekTime/1000);
                      cmd.putExtra("START_TIME",System.currentTimeMillis());
                      cmd.putExtra("ID",playerId);
                      mContext.sendBroadcast(cmd);
                      mSeekStartSend = true;
                  }else {
                      mContext.sendBroadcast(cmd);
                      mSeekStartSend = true;
                  }
            }
			// add by ysten-mark for heilongjiang sqm seekstart
			if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
				int position = getCurrentPosition()/1000;
				onEvent(HuaweiUtils.ACTION_SEEKSTART, HuaweiUtils.PARAM_POSITION + "=" + position);
			}
            mSeekNeedResend = false;
        }
			// end add by ysten-mark for heilongjiang sqm seekstart

        if(needosdvideo)
            setParameter(KEY_PARAMETER_AML_PLAYER_ENABLE_OSDVIDEO,"osdvideo:1");
        /*add for record play logs in file*/
        String mMesg = new String("start");
        save_log_in_file(PRINT_LOG_START,mMesg);

        stayAwake(true);
        _start();
        if(subtitleServiceDebug()) Log.i(TAG,"[start]mPath:"+mPath);
    }

    private native void _start() throws IllegalStateException;

    /**
     * Stops playback after playback has been stopped or paused.
     *
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    public void stop() throws IllegalStateException {
        stayAwake(false);
        subtitleClose();

	// gurantee buffer-seek in pair
	if(mBufferingStartSend) {
                long end = System.currentTimeMillis();
                cmd.putExtra("TYPE", "BUFFER_END");
                cmd.putExtra("END_TIME", end);
                if(null == info)
                    info= getMediaInfo();
                cmd.putExtra("ID", playerId);
                if((!mbIsFileDescriptor) && SystemProperties.get("sys.yst.mbufferstatus", "1").equals("0")){
                    SystemProperties.set("sys.yst.mbufferstatus", "1");
                    mContext.sendBroadcast(cmd);
                }
                mBufferingStartSend = false;
		// add by ysten-mark for heilongjiang sqm bufferend
		if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
			int position = getCurrentPosition()/1000;
			onEvent(HuaweiUtils.ACTION_BUFFEREND, HuaweiUtils.PARAM_POSITION + "=" + position);
		}
		// end add by ysten-mark for heilongjiang sqm bufferend
	}
        //add by chenfeng at 20191005:fix MEDIA_BLURREDSCREEN_END may not send
        if(mBlurredscreenStartSend) {
            Log.w(TAG, "media event: BLURREDSCREEN END\n");
            if(mContext != null) {
                int starttime = getCurrentPosition();
                if(null == info)
                    info = getMediaInfo();
                cmd.putExtra("TYPE", "BLURREDSCREEN_END");
                cmd.putExtra("ID", playerId);
                cmd.putExtra("END_TIME", System.currentTimeMillis());
                cmd.putExtra("RATIO", 1);
                if((!mbIsFileDescriptor)&& SystemProperties.get("sys.yst.mblurredscreenstatus", "1").equals("0")){
                    //begin by ysten.zhangjunjian,20191012 for blurred symbolic
                    SystemProperties.set("sys.yst.mblurredscreenstatus", "1");
                   //end by ysten.zhangjunjian,20191012 for blurred symbolic
                    mContext.sendBroadcast(cmd);
                    mBlurredscreenStartSend = false;
                }
            }
        }
    
        if(mUnloadStartSend) {
            Log.w(TAG, "media event: UNLOAD END\n");
            if(mContext != null) {
                int starttime = getCurrentPosition();
                if(null == info)
                    info = getMediaInfo();
                cmd.putExtra("TYPE", "UNLOAD_END");
                cmd.putExtra("ID", playerId);
                cmd.putExtra("END_TIME", System.currentTimeMillis());
                if((!mbIsFileDescriptor)&& SystemProperties.get("sys.yst.munloadstatus", "1").equals("0")){
                     SystemProperties.set("sys.yst.munloadstatus", "1");
                    mContext.sendBroadcast(cmd);
                    mUnloadStartSend = false;
                }
            }
        }


	if(mSeekStartSend) {
		  long end = System.currentTimeMillis();
                  cmd.putExtra("PLAY_TIME", mSeekTime/1000);
                  cmd.putExtra("TYPE", "SEEK_END");
                  cmd.putExtra("END_TIME", end);
                  if(null == info)
                      info= getMediaInfo();
                  cmd.putExtra("ID", playerId);
		  cmdSeek = cmd;
                  if(!mbIsFileDescriptor){
                      mContext.sendBroadcast(cmd);
		          mSeekStartSend = false;
                  }
			  mEventHandler.postDelayed(seekBufferEvent,8000);
	}

	//add by ysten-mark for heilongjiang sqm seekend
	if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
		int position = getCurrentPosition()/1000;
		onEvent(HuaweiUtils.ACTION_SEEKEND, HuaweiUtils.PARAM_POSITION + "=" + position );
    }
	//end add by ysten-mark for heilongjiang sqm seekend
		
        if(SystemProperties.getBoolean("media.player.report.udp", false) && mSendPrepareEvent && !mSendQuitEvent){
            long time = System.currentTimeMillis()/1000;
        if(null == info)
			info= getMediaInfo();
		StringBuffer buf=new StringBuffer();
        buf.append("<type>"+"Stop"+"</type>");
        buf.append('\n');
        buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
        buf.append('\n');
        buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
        sendDataReport(buf.toString(), buf.length());
        mSendQuitEvent = true;
        Log.e(TAG, buf.length()+" ,:"+buf.toString());
        }else if(mContext != null && mSendPrepareEvent && !mSendQuitEvent){
			int starttime = getCurrentPosition();
            Log.d(TAG , "starttime is : "+starttime/1000);
            cmd.putExtra("TYPE", "PLAY_QUIT");
			mSeekFlag=false;
			mPlayStartFlag=false; 
            cmd.putExtra("PLAY_TIME", starttime/1000);
            cmd.putExtra("TIME", System.currentTimeMillis());
            if(null == info)
                info= getMediaInfo();
                cmd.putExtra("ID", playerId);
                Log.d(TAG , "amPlayer id is : "+info.player_id);
                Log.d(TAG , "player id is : "+playerId);
                //begin:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
                if(!mbIsFileDescriptor){
                if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
                || SystemProperties.get("ro.ysten.province","master").contains("jiangsu")
                || SystemProperties.get("ro.ysten.province","master").contains("anhui")
                || SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")
                || SystemProperties.get("ro.ysten.province","master").contains("CM201_guangdong_zhuoying")
                || SystemProperties.get("ro.ysten.province","master").contains("ningxia") ){
                    if(!mQuitFlag){
                        mContext.sendBroadcast(cmd);
						mSendPrepareEvent = false;
	         Log.e(TAG, "dbg://2641 mSendPrepareEvent:"+mSendPrepareEvent);							
                        mSendQuitEvent = true;
			mQuitFlag=true;
                 }
		   }else{
                        mContext.sendBroadcast(cmd);
						mSendPrepareEvent = false;
	         Log.e(TAG, "dbg://2648 mSendPrepareEvent:"+mSendPrepareEvent);							
                        mSendQuitEvent = true;
		   }
		   // add by ysten-mark for heilongjiang sqm played
		   	if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                onEvent(HuaweiUtils.ACTION_PLAYEND,  HuaweiUtils.PARAM_ENDREASON + "=" + HuaweiUtils.ENDREASON_CLOSED); 
                close();
            }
		   // end add by ysten-mark for heilongjiang sqm played
                }
                //end:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
				
				// huawei plugin start {{
	
            }

        /*add for record play logs in file*/
        if(!is_print_stop_finalize)
            try {
                synchronized (mSaveLogLock) {
					if(! DEFAULT_LOGFILE.equals(logfile_name)){
						    String mMsg = new String(" call stop\r\n destory this player\r\n");
							byte log_buf[] = mMsg.getBytes();
							Log.e(TAG, "mMsg: " + mMsg);
							File fp = new File(RECORD_PATH + File.separator + logfile_name);
							OutputStream out = null;
							out = new FileOutputStream(fp,true);
							for (int i = 0; i < log_buf.length; i++){
								out.write(log_buf[i]);
							}
							out.close();
							mMsg = "";
							//Runtime.getRuntime().exec("chmod 666 " + RECORD_PATH+File.separator+logfile_name);
							is_print_stop_finalize = true;
					}
		        }
			} catch (Exception e ){
				    e.printStackTrace();
			}

        _stop();
    }

    private native void _stop() throws IllegalStateException;

    private Runnable seekBufferEvent = new Runnable() {
	@Override
	public void run() {
	    mSeekFlag=false;
	}
    };
    
    //add by chenfeng at 20191005:limit MEDIA_BLURREDSCREEN_START
    private Runnable blurredscreenStartEvent = new Runnable() {
	@Override
	public void run() {
	    mBlurredscreenStartCnt = 0;
	}
    };
    
    //add by chenfeng at 20191011:limit MEDIA_BLURREDSCREEN_START total time
    private Runnable blurredscreenStartTimeEvent = new Runnable() {
        @Override
        public void run() {
            if (mEventHandler != null) {
                Message m = mEventHandler.obtainMessage(MEDIA_BLURREDSCREEN_END);
                mEventHandler.sendMessage(m);
            }
        }
    };
   
    //add by chenfeng at 20191011:limit MEDIA_UNLOAD_END total time
    private Runnable unloadStartTimeEvent = new Runnable() {
        @Override
        public void run() {
            if (mEventHandler != null) {
                Message m = mEventHandler.obtainMessage(MEDIA_UNLOAD_END);
                mEventHandler.sendMessage(m);
            }
        }
    };

   //ysten.zhangjunjian,for upload end event for guangdong
   public void shutdownStop(Context mContext){
		cmd = new Intent("MEDIA_PLAY_MONITOR_MESSAGE");
		int detectid = 0;
                Log.d(TAG,"....."+SystemProperties.get("sys.yst.mbufferstatus", "1")+"+++++"+SystemProperties.get("sys.yst.munloadstatus", "1")+"==="+SystemProperties.get("sys.yst.mblurredscreenstatus", "1"));
                if(null==info) info=getMediaInfo();
			if(SystemProperties.get("sys.yst.mbufferstatus", "1").equals("0")){
				long end = System.currentTimeMillis();
				cmd.putExtra("TYPE", "BUFFER_END");
				cmd.putExtra("END_TIME", end);
				detectid = Integer.valueOf(SystemProperties.get("sys.yst.softdetectid", "1"));
				cmd.putExtra("ID", detectid);
				mContext.sendBroadcast(cmd);
				Log.d(TAG, "zhangjunjian----BUFFER_END in shutdownStop");
				mBufferingStartSend = false;
				SystemProperties.set("sys.yst.mbufferstatus", "1");
			}
		
			if(SystemProperties.get("sys.yst.munloadstatus", "1").equals("0")){
				int starttime = getCurrentPosition();
				cmd.putExtra("TYPE", "UNLOAD_END");
				detectid = Integer.valueOf(SystemProperties.get("sys.yst.softdetectid", "1"));
				cmd.putExtra("ID", detectid);
				cmd.putExtra("END_TIME", System.currentTimeMillis());
				mContext.sendBroadcast(cmd); 
				Log.d(TAG, "zhangjunjian----UNLOAD_END in shutdownStop");
				//mUnloadStatus = 1;
				SystemProperties.set("sys.yst.munloadstatus", "1");
			}
			if(SystemProperties.get("sys.yst.mblurredscreenstatus", "1").equals("0")) {                    
		        	int starttime = getCurrentPosition();                        
		        	cmd.putExtra("TYPE", "BLURREDSCREEN_END");
	                        detectid = Integer.valueOf(SystemProperties.get("sys.yst.softdetectid", "1"));			
		        	cmd.putExtra("ID", detectid);                          
		        	cmd.putExtra("END_TIME", System.currentTimeMillis());        
		        	int random = (int)(2+Math.random()*10);                                                                        
		        	cmd.putExtra("RATIO", random);                             
		        	mContext.sendBroadcast(cmd);
				Log.d(TAG, "zhangjunjian----BLURREDSCREEN END in shutdownStop");				
		        	//mBlurredscreenStatus = 1; 
				SystemProperties.set("sys.yst.mblurredscreenstatus", "1");
			}
			int starttime = getCurrentPosition();
                        cmd = new Intent("MEDIA_PLAY_MONITOR_MESSAGE");			
			cmd.putExtra("TYPE", "PLAY_QUIT");                    
			cmd.putExtra("PLAY_TIME", starttime/1000);            
			cmd.putExtra("TIME", System.currentTimeMillis());
		       detectid = Integer.valueOf(SystemProperties.get("sys.yst.softdetectid", "1"));
                        SystemProperties.get("sys.yst.softdetectid", "1");	
			cmd.putExtra("ID", detectid);                   
			Log.d(TAG , "player id is : "+detectid);        
			Log.e(TAG, "zhangjunjian----test----PLAY_QUIT-shutdownStop");
			mContext.sendBroadcast(cmd);                          
			mSendQuitEvent = true;                                
			//mHuiKanFirstLoad = 0;
	}
	//end by ysten.zhangjunjian ,for upload event for guangdong 
    /**
     * Pauses playback. Call start() to resume.
     *
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    public void pause() throws IllegalStateException {
        stayAwake(false);
		/*add for record play logs in file*/
		String mMesg = new String("pause");
		save_log_in_file(PRINT_LOG_PAUSE,mMesg);
		
        _pause();
    }

    private native void _pause() throws IllegalStateException;

    private native Bitmap getSubtitleBitmap();
    /**
     * Set the low-level power management behavior for this MediaPlayer.  This
     * can be used when the MediaPlayer is not playing through a SurfaceHolder
     * set with {@link #setDisplay(SurfaceHolder)} and thus can use the
     * high-level {@link #setScreenOnWhilePlaying(boolean)} feature.
     *
     * <p>This function has the MediaPlayer access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link android.os.PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     *
     * @param context the Context to use
     * @param mode    the power/wake mode to set
     * @see android.os.PowerManager
     */
    public void setWakeMode(Context context, int mode) {
        boolean washeld = false;
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    /**
     * Control whether we should use the attached SurfaceHolder to keep the
     * screen on while video playback is occurring.  This is the preferred
     * method over {@link #setWakeMode} where possible, since it doesn't
     * require that the application have permission for low-level wake lock
     * access.
     *
     * @param screenOn Supply true to keep the screen on, false to allow it
     * to turn off.
     */
    public void setScreenOnWhilePlaying(boolean screenOn) {
        if (mScreenOnWhilePlaying != screenOn) {
            if (screenOn && mSurfaceHolder == null) {
                Log.w(TAG, "setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
            }
            mScreenOnWhilePlaying = screenOn;
            updateSurfaceScreenOn();
        }
    }

    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    /**
     * Returns the width of the video.
     *
     * @return the width of the video, or 0 if there is no video,
     * no display surface was set, or the width has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the width is available.
     */
    public native int getVideoWidth();

    /**
     * Returns the height of the video.
     *
     * @return the height of the video, or 0 if there is no video,
     * no display surface was set, or the height has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the height is available.
     */
    public native int getVideoHeight();

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     * @throws IllegalStateException if the internal player engine has not been
     * initialized or has been released.
     */
    public native boolean isPlaying();

    /**
     * Seeks to specified time position.
     *
     * @param msec the offset in milliseconds from the start to seek to
     * @throws IllegalStateException if the internal player engine has not been
     * initialized
     */

    public native void _seekTo(int msec) throws IllegalStateException;
	
    public  void seekTo(int msec) throws IllegalStateException {
        mSeekTime = msec;
        _seekTo(msec);
    }

    /**
     * Gets the current playback position.
     *
     * @return the current position in milliseconds
     */
    public native int getCurrentPosition();

    /**
     * Gets the duration of the file.
     *
     * @return the duration in milliseconds, if no duration is available
     *         (for example, if streaming live content), -1 is returned.
     */
    public native int getDuration();

    /**
     * Gets the media metadata.
     *
     * @param update_only controls whether the full set of available
     * metadata is returned or just the set that changed since the
     * last call. See {@see #METADATA_UPDATE_ONLY} and {@see
     * #METADATA_ALL}.
     *
     * @param apply_filter if true only metadata that matches the
     * filter is returned. See {@see #APPLY_METADATA_FILTER} and {@see
     * #BYPASS_METADATA_FILTER}.
     *
     * @return The metadata, possibly empty. null if an error occured.
     // FIXME: unhide.
     * {@hide}
     */
    public Metadata getMetadata(final boolean update_only,
                                final boolean apply_filter) {
        Parcel reply = Parcel.obtain();
        Metadata data = new Metadata();

        if (!native_getMetadata(update_only, apply_filter, reply)) {
            reply.recycle();
            return null;
        }

        // Metadata takes over the parcel, don't recycle it unless
        // there is an error.
        if (!data.parse(reply)) {
            reply.recycle();
            return null;
        }
        return data;
    }

    /**
     * Set a filter for the metadata update notification and update
     * retrieval. The caller provides 2 set of metadata keys, allowed
     * and blocked. The blocked set always takes precedence over the
     * allowed one.
     * Metadata.MATCH_ALL and Metadata.MATCH_NONE are 2 sets available as
     * shorthands to allow/block all or no metadata.
     *
     * By default, there is no filter set.
     *
     * @param allow Is the set of metadata the client is interested
     *              in receiving new notifications for.
     * @param block Is the set of metadata the client is not interested
     *              in receiving new notifications for.
     * @return The call status code.
     *
     // FIXME: unhide.
     * {@hide}
     */
    public int setMetadataFilter(Set<Integer> allow, Set<Integer> block) {
        // Do our serialization manually instead of calling
        // Parcel.writeArray since the sets are made of the same type
        // we avoid paying the price of calling writeValue (used by
        // writeArray) which burns an extra int per element to encode
        // the type.
        Parcel request =  newRequest();

        // The parcel starts already with an interface token. There
        // are 2 filters. Each one starts with a 4bytes number to
        // store the len followed by a number of int (4 bytes as well)
        // representing the metadata type.
        int capacity = request.dataSize() + 4 * (1 + allow.size() + 1 + block.size());

        if (request.dataCapacity() < capacity) {
            request.setDataCapacity(capacity);
        }

        request.writeInt(allow.size());
        for(Integer t: allow) {
            request.writeInt(t);
        }
        request.writeInt(block.size());
        for(Integer t: block) {
            request.writeInt(t);
        }
        return native_setMetadataFilter(request);
    }

    /**
     * Set the MediaPlayer to start when this MediaPlayer finishes playback
     * (i.e. reaches the end of the stream).
     * The media framework will attempt to transition from this player to
     * the next as seamlessly as possible. The next player can be set at
     * any time before completion. The next player must be prepared by the
     * app, and the application should not call start() on it.
     * The next MediaPlayer must be different from 'this'. An exception
     * will be thrown if next == this.
     * The application may call setNextMediaPlayer(null) to indicate no
     * next player should be started at the end of playback.
     * If the current player is looping, it will keep looping and the next
     * player will not be started.
     *
     * @param next the player to start after this one completes playback.
     *
     */
    public native void setNextMediaPlayer(MediaPlayer next);

    /**
     * Releases resources associated with this MediaPlayer object.
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer. In particular, whenever an Activity
     * of an application is paused (its onPause() method is called),
     * or stopped (its onStop() method is called), this method should be
     * invoked to release the MediaPlayer object, unless the application
     * has a special need to keep the object around. In addition to
     * unnecessary resources (such as memory and instances of codecs)
     * being held, failure to call this method immediately if a
     * MediaPlayer object is no longer needed may also lead to
     * continuous battery consumption for mobile devices, and playback
     * failure for other applications if no multiple instances of the
     * same codec are supported on a device. Even if multiple instances
     * of the same codec are supported, some performance degradation
     * may be expected when unnecessary multiple instances are used
     * at the same time.
     */
    public void release() {
        if(SystemProperties.getBoolean("media.player.report.udp", false) && mSendPrepareEvent && !mSendQuitEvent){
            long time = System.currentTimeMillis()/1000;
            if(null == info)
                info= getMediaInfo();
            StringBuffer buf=new StringBuffer();
            buf.append("<type>"+"Stop"+"</type>");
            buf.append('\n');
            buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
            buf.append('\n');
            buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
            sendDataReport(buf.toString(), buf.length());
            mSendQuitEvent = true;
            Log.e(TAG, buf.length()+" ,:"+buf.toString());
       }else {
            //add by chenfeng at 20191005:fix MEDIA_BLURREDSCREEN_END may not send
            if(mBufferingStartSend) {
                long end = System.currentTimeMillis();
                cmd.putExtra("TYPE", "BUFFER_END");
                cmd.putExtra("END_TIME", end);
                SystemProperties.set("sys.yst.mbufferstatus", "1");
                if(null == info)
                    info= getMediaInfo();
                cmd.putExtra("ID", playerId);
                if(!mbIsFileDescriptor){
                    mContext.sendBroadcast(cmd);
                }
                mBufferingStartSend = false;
            }
            if(mBlurredscreenStartSend) {
                Log.w(TAG, "media event: BLURREDSCREEN END\n");
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "BLURREDSCREEN_END");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
                    cmd.putExtra("RATIO", 1);
                    SystemProperties.set("sys.yst.mblurredscreenstatus", "1");
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                        mBlurredscreenStartSend = false;
                    }
                }
            }
            if(mUnloadStartSend) {
                Log.w(TAG, "media event: UNLOAD END\n");
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "UNLOAD_END");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
                    SystemProperties.set("sys.yst.munloadstatus", "1");
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                        mUnloadStartSend = false;
                    }
                }
            }
            if(mContext != null && mSendPrepareEvent && !mSendQuitEvent){
                int starttime = getCurrentPosition();
                Log.d(TAG , "starttime is : "+starttime/1000);
                cmd.putExtra("TYPE", "PLAY_QUIT");
		 mSeekFlag=false;
		 mPlayStartFlag=false; 
                cmd.putExtra("PLAY_TIME", starttime/1000);
                cmd.putExtra("TIME", System.currentTimeMillis());
                if(null == info)
                    info= getMediaInfo();
                cmd.putExtra("ID", playerId);
                Log.d(TAG , "amPlayer id is : "+info.player_id);
                Log.d(TAG , "player id is : "+playerId);
                //begin:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
                if(!mbIsFileDescriptor){
		    if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
				|| SystemProperties.get("ro.ysten.province","master").contains("jiangsu")
                || SystemProperties.get("ro.ysten.province","master").contains("ningxia")
				|| SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")
                || SystemProperties.get("ro.ysten.province","master").contains("CM201_guangdong_zhuoying")
				|| SystemProperties.get("ro.ysten.province","master").contains("anhui")){
			if(!mQuitFlag){
                            mContext.sendBroadcast(cmd);
                            mSendQuitEvent = true;
			    mQuitFlag=true;
			}
                    }else{
                        mContext.sendBroadcast(cmd);
                        mSendQuitEvent = true;					
		    }
			//add by ysten-mark for heilongjiang sqm playend
			if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                onEvent(HuaweiUtils.ACTION_PLAYEND,  HuaweiUtils.PARAM_ENDREASON + "=" + HuaweiUtils.ENDREASON_CLOSED); 
                close();
            }
			//end add by ysten-mark for heilongjiang sqm playend
		}
                //end:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
            }
			
		}
        stayAwake(false);
        updateSurfaceScreenOn();
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
        mOnVideoSizeChangedListener = null;
        mOnTimedTextListener = null;

        if (mTimeProvider != null) {
            mTimeProvider.close();
            mTimeProvider = null;
        }
        mOnSubtitleDataListener = null;
        mOnBlurayInfoListener = null;
        subtitleClose();
        subtitleServiceRelease();
        if (mSavelogLooper != null) {
            mSavelogLooper.quitSafely();
            mSavelogLooper = null;
        }
        _release();
        /*if((mMountReceiver != null) && (mContext != null)) {
            mContext.unregisterReceiver(mMountReceiver);
            mMountReceiver = null;
        }*/

        if (mContext != null)
            mContext = null;

        if (mSurfaceHolder != null)
            mSurfaceHolder = null;
    }

    private native void _release();

    /**
     * Resets the MediaPlayer to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     */
    public void reset() {
		Log.d(TAG , "-----MediaPlayer into reset : ");
		Log.d(TAG , "-----MediaPlayer media.player.report.udp : "+SystemProperties.getBoolean("media.player.report.udp", false));
		Log.d(TAG , "-----MediaPlayer mSendPrepareEvent : "+mSendPrepareEvent);
		Log.d(TAG , "-----MediaPlayer !mSendQuitEvent : "+!mSendQuitEvent);

        if(SystemProperties.getBoolean("media.player.report.udp", false) && mSendPrepareEvent && !mSendQuitEvent){
            long time = System.currentTimeMillis()/1000;
            if(null == info)
                info= getMediaInfo();
            StringBuffer buf=new StringBuffer();
            buf.append("<type>"+"Stop"+"</type>");
            buf.append('\n');
            buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
            buf.append('\n');
            buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
            sendDataReport(buf.toString(), buf.length());
            mSendQuitEvent = true;
            Log.e(TAG, buf.length()+" ,:"+buf.toString());
        }else {
            //add by chenfeng at 20191005:fix MEDIA_BLURREDSCREEN_END may not send
            if(mBufferingStartSend) {
                long end = System.currentTimeMillis();
                cmd.putExtra("TYPE", "BUFFER_END");
                cmd.putExtra("END_TIME", end);
                if(null == info)
                    info= getMediaInfo();
                cmd.putExtra("ID", playerId);
               if(!mbIsFileDescriptor){
                    mContext.sendBroadcast(cmd);
                }
                mBufferingStartSend = false;
            }
           if(mBlurredscreenStartSend) {
                Log.w(TAG, "media event: BLURREDSCREEN END\n");
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "BLURREDSCREEN_END");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
                    cmd.putExtra("RATIO", 1);
                     SystemProperties.set("sys.yst.mblurredscreenstatus", "1");
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                        mBlurredscreenStartSend = false;
                    }
                }
            }
            if(mUnloadStartSend) {
                Log.w(TAG, "media event: UNLOAD END\n");
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                   if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "UNLOAD_END");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
                     SystemProperties.set("sys.yst.munloadstatus", "1");
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                        mUnloadStartSend = false;
                    }
                }
            } 
			
		if(mContext != null && mSendPrepareEvent){
            int starttime = getCurrentPosition();
            Log.d(TAG , "starttime is : "+starttime/1000);
            cmd.putExtra("TYPE", "PLAY_QUIT");
	     mSeekFlag=false;
	     mPlayStartFlag=false; 
            cmd.putExtra("PLAY_TIME", starttime/1000);
            cmd.putExtra("TIME", System.currentTimeMillis());
            if(null == info)
                info= getMediaInfo();
            cmd.putExtra("ID", playerId);
            Log.d(TAG , "amPlayer id is : "+info.player_id);
            Log.d(TAG , "player id is : "+playerId);
            //begin:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
            if(!mbIsFileDescriptor){
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
			|| SystemProperties.get("ro.ysten.province","master").contains("jiangsu")
			|| SystemProperties.get("ro.ysten.province","master").contains("anhui")
			|| SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")
            || SystemProperties.get("ro.ysten.province","master").contains("CM201_guangdong_zhuoying")
            || SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
		    if(!mQuitFlag){
                         mContext.sendBroadcast(cmd);
                         mSendQuitEvent = true;
                         mQuitFlag=true;
		    }
                }else{
                    mContext.sendBroadcast(cmd);
                    mSendQuitEvent = true;			
		}
			//add by ysten-mark for heilongjiang sqm playend
		if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                onEvent(HuaweiUtils.ACTION_PLAYEND,  HuaweiUtils.PARAM_ENDREASON + "=" + HuaweiUtils.ENDREASON_CLOSED); 
                close();
        }
			//add by ysten-mark for heilongjiang sqm playend
	    }
            //end:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
        }
		}
        mSendPrepareEvent = false;
	         Log.e(TAG, "dbg://3313 mSendPrepareEvent:"+mSendPrepareEvent);			
        mSetDataSourceTime = 0;
        mReportTimeOffset = 0;
	    mBufferStartFlag = false;
        mSelectedSubtitleTrackIndex = -1;
        synchronized(mOpenSubtitleSources) {
            for (final InputStream is: mOpenSubtitleSources) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            mOpenSubtitleSources.clear();
        }
        mOutOfBandSubtitleTracks.clear();
        mInbandSubtitleTracks = new SubtitleTrack[0];
        if (mSubtitleController != null) {
            mSubtitleController.reset();
        }
        if (mTimeProvider != null) {
            mTimeProvider.close();
            mTimeProvider = null;
        }

        stayAwake(false);
        _reset();
        // make sure none of the listeners get called anymore
        if (mEventHandler != null) {
            mEventHandler.removeCallbacksAndMessages(null);
        }

        disableProxyListener();
    }

    private native void _reset();

    /**
     * Sets the audio stream type for this MediaPlayer. See {@link AudioManager}
     * for a list of stream types. Must call this method before prepare() or
     * prepareAsync() in order for the target stream type to become effective
     * thereafter.
     *
     * @param streamtype the audio stream type
     * @see android.media.AudioManager
     */
    public native void setAudioStreamType(int streamtype);

    /**
     * Sets the player to be looping or non-looping.
     *
     * @param looping whether to loop or not
     */
    public native void setLooping(boolean looping);

    /**
     * Checks whether the MediaPlayer is looping or non-looping.
     *
     * @return true if the MediaPlayer is currently looping, false otherwise
     */
    public native boolean isLooping();

    /**
     * Sets the volume on this player.
     * This API is recommended for balancing the output of audio streams
     * within an application. Unless you are writing an application to
     * control user settings, this API should be used in preference to
     * {@link AudioManager#setStreamVolume(int, int, int)} which sets the volume of ALL streams of
     * a particular type. Note that the passed volume values are raw scalars in range 0.0 to 1.0.
     * UI controls should be scaled logarithmically.
     *
     * @param leftVolume left volume scalar
     * @param rightVolume right volume scalar
     */
    /*
     * FIXME: Merge this into javadoc comment above when setVolume(float) is not @hide.
     * The single parameter form below is preferred if the channel volumes don't need
     * to be set independently.
     */
    public native void setVolume(float leftVolume, float rightVolume);

    /**
     * Similar, excepts sets volume of all channels to same value.
     * @hide
     */
    public void setVolume(float volume) {
        setVolume(volume, volume);
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId the audio session ID.
     * The audio session ID is a system wide unique identifier for the audio stream played by
     * this MediaPlayer instance.
     * The primary use of the audio session ID  is to associate audio effects to a particular
     * instance of MediaPlayer: if an audio session ID is provided when creating an audio effect,
     * this effect will be applied only to the audio content of media players within the same
     * audio session and not to the output mix.
     * When created, a MediaPlayer instance automatically generates its own audio session ID.
     * However, it is possible to force this player to be part of an already existing audio session
     * by calling this method.
     * This method must be called before one of the overloaded <code> setDataSource </code> methods.
     * @throws IllegalStateException if it is called in an invalid state
     */
    public native void setAudioSessionId(int sessionId)  throws IllegalArgumentException, IllegalStateException;

    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID. {@see #setAudioSessionId(int)}
     * Note that the audio session ID is 0 only if a problem occured when the MediaPlayer was contructed.
     */
    public native int getAudioSessionId();

    /**
     * Attaches an auxiliary effect to the player. A typical auxiliary effect is a reverberation
     * effect which can be applied on any sound source that directs a certain amount of its
     * energy to this effect. This amount is defined by setAuxEffectSendLevel().
     * {@see #setAuxEffectSendLevel(float)}.
     * <p>After creating an auxiliary effect (e.g.
     * {@link android.media.audiofx.EnvironmentalReverb}), retrieve its ID with
     * {@link android.media.audiofx.AudioEffect#getId()} and use it when calling this method
     * to attach the player to the effect.
     * <p>To detach the effect from the player, call this method with a null effect id.
     * <p>This method must be called after one of the overloaded <code> setDataSource </code>
     * methods.
     * @param effectId system wide unique id of the effect to attach
     */
    public native void attachAuxEffect(int effectId);

    /* Do not change these values (starting with KEY_PARAMETER) without updating
     * their counterparts in include/media/mediaplayer.h!
     */

    // There are currently no defined keys usable from Java with get*Parameter.
    // But if any keys are defined, the order must be kept in sync with include/media/mediaplayer.h.
    // private static final int KEY_PARAMETER_... = ...;

    /*
    info video surface layout info,
    format:left=%d;top=%d;right=%d,...
    */
    public static final int KEY_PARAMETER_AML_VIDEO_POSITION_INFO = 2000;

    /*
    AMLOGIC_PLAYER?
    or others
    */
    public static final int KEY_PARAMETER_AML_PLAYER_TYPE_STR = 2001;


    /*
        int value;
    */
    public static final int KEY_PARAMETER_AML_PLAYER_VIDEO_OUT_TYPE = 2002;

    //amlogic private API,set only.
    //switch sound track
    public static final int KEY_PARAMETER_AML_PLAYER_SWITCH_SOUND_TRACK = 2003;//string,refer to lmono,rmono,stereo,set only
    //switch audio track
    public static final int KEY_PARAMETER_AML_PLAYER_SWITCH_AUDIO_TRACK = 2004;//string,refer to audio track index,set only

    public static final int KEY_PARAMETER_AML_PLAYER_TRICKPLAY_FORWARD=2005;//string,refer to forward:speed
    public static final int KEY_PARAMETER_AML_PLAYER_TRICKPLAY_BACKWARD=2006;//string,refer to  backward:speed
    public static final int KEY_PARAMETER_AML_PLAYER_FORCE_HARD_DECODE=2007;//string,refer to mp3,etc.
    public static final int KEY_PARAMETER_AML_PLAYER_FORCE_SOFT_DECODE=2008;//string,refer to mp3,etc.
    private static final int KEY_PARAMETER_AML_PLAYER_GET_MEDIA_INFO = 2009;//string,get media info
    private static final int KEY_PARAMETER_AML_PLAYER_GET_REPORT_PARAM = 4000;//for report param
    /**
    * @hide
    */
    public static final int KEY_PARAMETER_AML_PLAYER_FORCE_SCREEN_MODE = 2010;//string,set screen mode
    /**
    * @hide
    */
    public static final int KEY_PARAMETER_AML_PLAYER_SET_DISPLAY_MODE = 2011;//string,set display mode 3D
    public static final int KEY_PARAMETER_AML_PLAYER_SET_DISP_LASTFRAME = 2014;//string,set display last frame

    /**
    * @hide
    */
    public static final int KEY_PARAMETER_AML_PLAYER_SWITCH_VIDEO_TRACK = 2015;//string,refer to video track index,set only

    public static final int KEY_PARAMETER_AML_PLAYER_ENABLE_OSDVIDEO = 8003;  //string,play enable osd video for this player....
    /**
    * @hide
    */
    public static final int KEY_PARAMETER_AML_PLAYER_GET_DTS_ASSET_TOTAL = 2012;//string, get dts asset total number
    /**
    * @hide
    */
    public static final int KEY_PARAMETER_AML_PLAYER_SET_DTS_ASSET = 2013;//string, set dts asset
    public static final int KEY_PARAMETER_AML_PLAYER_PR_CUSTOM_DATA=9001;//string, playready, set only
    public static final int VIDEO_OUT_SOFT_RENDER = 0;
    public static final int VIDEO_OUT_HARDWARE  =   1;

    private static final int KEY_PARAMETER_AML_PLAYER_GET_REAL_POSITION = 2301;//int,get real current position

    /**
     * Sets the parameter indicated by key.
     * @param key key indicates the parameter to be set.
     * @param value value of the parameter to be set.
     * @return true if the parameter is set successfully, false otherwise
     * {@hide}
     */
    public native boolean setParameter(int key, Parcel value);

    /**
     * Sets the parameter indicated by key.
     * @param key key indicates the parameter to be set.
     * @param value value of the parameter to be set.
     * @return true if the parameter is set successfully, false otherwise
     * {@hide}
     */
    public boolean setParameter(int key, String value) {
        Parcel p = Parcel.obtain();
        p.writeString(value);
        boolean ret = setParameter(key, p);
        p.recycle();
        return ret;
    }

    /**
     * Sets the parameter indicated by key.
     * @param key key indicates the parameter to be set.
     * @param value value of the parameter to be set.
     * @return true if the parameter is set successfully, false otherwise
     * {@hide}
     */
    public boolean setParameter(int key, int value) {
        Parcel p = Parcel.obtain();
        p.writeInt(value);
        boolean ret = setParameter(key, p);
        p.recycle();
        return ret;
    }

    /*
     * Gets the value of the parameter indicated by key.
     * @param key key indicates the parameter to get.
     * @param reply value of the parameter to get.
     */
    private native void getParameter(int key, Parcel reply);

    /**
    * @hide
    */
    public class VideoInfo{
        public int index;
        public int id;
        public String vformat;
        public int width;
        public int height;
    }

	/**
    * @hide
    */
    public class AudioInfo{
        public int index;
        public int id; //id is useless for application
        public int aformat;
        public int channel;
        public int sample_rate;
    }

	/**
    * @hide
    */
    public class SubtitleInfo{
        public int index;
        public int id;
        public int sub_type;
        public String sub_language;
    }

    /**
    * @hide
    */
    public class TsProgrameInfo{
        public int v_pid;
        public String title;
    }

	/*hls module para*/
	public class ReportParam_Hls {
		public int bitrate;
		public int ts_get_delay_max_time;
		public int ts_get_delay_avg_time;
		public int ts_get_suc_times;
		public int ts_get_times;

		public int m3u8_get_delay_avg_time;
		public int m3u8_get_delay_max_time;

		public String m3u8_server;//ip:port
		public String ts_server; //ip:port
	}

	/**
    * @hide
    */
    public class MediaInfo{
        public int player_id;
        public String filename;
        public String url;
        public int duration;
        public String file_size;
        public int bitrate;
        public int type;
        public int fps;
        public int cur_video_index;
        public int cur_audio_index;
        public int cur_sub_index;

        public int total_video_num;
        public VideoInfo[] videoInfo;

        public int total_audio_num;
        public AudioInfo[] audioInfo;

        public int total_sub_num;
        public SubtitleInfo[] subtitleInfo;

        public int total_ts_num;
        public TsProgrameInfo[] tsprogrameInfo;

        public int carton_times;
        public int carton_time;
        public int first_pic_time;
        public ReportParam_Hls hls_para;

        //video info
        public String vformat;
        public int video_aspect;
        public int video_ratio;
        public int progress;
        public int vbuf_size;
        public int vbuf_used_size;
        public int vdec_error;
        public int vdec_drop;
        public int vdec_underflow;
        public int vdec_pts_error;

        //audio info
        public String aformat;
        public int audio_bitrate;
        public int audio_channels;
        public int audio_sr;
        public int abuf_size;
        public int abuf_used_size;
        public int adec_error;
        public int adec_drop;
        public int adec_underflow;
        public int adec_pts_error;
        public String audio_sub_language;

        public int ts_cc_discont;
        public int ts_sync_lost_num;
        public int avpts_diff;
        public String transport_protocol;
    }

	/**
		update media info
	*/
	private int updateMediaReportParam() {
		int ret = 1;

		if(mContext == null) {
			Log.e(TAG,"[updateMediaReportParam error] no context");
			return 0;
		}

		/*Begin: Update MediaInfo*/
		if (null == info)
			info= getMediaInfo();

		if (info.hls_para == null)
			info.hls_para = new ReportParam_Hls();

		Parcel p = Parcel.obtain();
		try{
			getParameter(KEY_PARAMETER_AML_PLAYER_GET_REPORT_PARAM, p);
		} catch (Exception e ){
		    Log.i(TAG,"[getMediaInfo error] use info:"+info);
		    p.recycle();
		    return 0;
		}

		info.first_pic_time = p.readInt();
		info.carton_times = p.readInt();
		info.carton_time = p.readInt();

		info.hls_para.bitrate = p.readInt();
		info.hls_para.m3u8_get_delay_max_time = p.readInt();
		info.hls_para.m3u8_get_delay_avg_time = p.readInt();
		info.hls_para.ts_get_delay_max_time = p.readInt();
		info.hls_para.ts_get_delay_avg_time = p.readInt();
		info.hls_para.ts_get_suc_times = p.readInt();
		info.hls_para.ts_get_times = p.readInt();
		info.hls_para.m3u8_server = p.readString();
		info.hls_para.ts_server = p.readString();

		//current video info
		info.vformat = p.readString();
		info.video_aspect = p.readInt();
		info.video_ratio = p.readInt();
		info.progress = p.readInt();
		info.vbuf_size = p.readInt();
		info.vbuf_used_size = p.readInt();
		info.vdec_error = p.readInt();
		info.vdec_drop = p.readInt();
		info.vdec_underflow = p.readInt();
		info.vdec_pts_error = p.readInt();

		//current audio info
		info.aformat = p.readString();
		info.audio_bitrate = p.readInt();
		info.audio_channels = p.readInt();
		info.audio_sr = p.readInt();
		info.abuf_size = p.readInt();
		info.abuf_used_size = p.readInt();
		info.adec_error = p.readInt();
		info.adec_drop = p.readInt();
		info.adec_underflow = p.readInt();
		info.adec_pts_error = p.readInt();
		info.audio_sub_language = p.readString();

		//ts program info
		info.ts_cc_discont = p.readInt();
		info.ts_sync_lost_num = p.readInt();

		//other info
		info.avpts_diff = p.readInt();
		info.transport_protocol = p.readString();

		p.recycle();
		/*End*/

		if (it_report_unicom != null) {
			it_report_unicom.putExtra("FIRST_PIC_TIME", info.first_pic_time);
			it_report_unicom.putExtra("VIDEO_RESOLUTION", ""+info.videoInfo[0].width+ "x"+info.videoInfo[0].height);
			it_report_unicom.putExtra("FRAMERATE", info.fps);
			it_report_unicom.putExtra("BITRATE", info.hls_para.bitrate);
			it_report_unicom.putExtra("CATON_TIME", info.carton_time);
			it_report_unicom.putExtra("CATON_TIMES", info.carton_times);
			it_report_unicom.putExtra("TS_GET_DELAY_MAX_TIME", info.hls_para.ts_get_delay_max_time);
			it_report_unicom.putExtra("TS_GET_DELAY_AVG_TIME", info.hls_para.ts_get_delay_avg_time);
			it_report_unicom.putExtra("TS_GET_SUC_TIMES", info.hls_para.ts_get_suc_times);
			it_report_unicom.putExtra("TS_GET_TIMES", info.hls_para.ts_get_times);
			it_report_unicom.putExtra("M3U8_GET_DELAY_AVG_TIME", info.hls_para.m3u8_get_delay_avg_time);
			it_report_unicom.putExtra("M3U8_GET_DELAY_MAX_TIME", info.hls_para.m3u8_get_delay_avg_time);
			it_report_unicom.putExtra("M3U8_SERVER", info.hls_para.m3u8_server);
			it_report_unicom.putExtra("TS_SERVER", info.hls_para.ts_server);
			Log.i(TAG, "<FPT:" + info.first_pic_time
				+ "|VR:" + info.videoInfo[0].width+ "x"+info.videoInfo[0].height
				+ "|FPS:" + info.fps + "|BR:" + info.hls_para.bitrate
				+ "|CT:" + info.carton_time
				+ "|CTS:" + info.carton_times
				+ "|TGDMT:" + info.hls_para.ts_get_delay_max_time
				+ "|TGDAT:" + info.hls_para.ts_get_delay_avg_time
				+ "|TGST:" + info.hls_para.ts_get_suc_times
				+ "|TGT:" + info.hls_para.ts_get_times
				+ "|MGDAT:" + info.hls_para.m3u8_get_delay_avg_time
				+ "|MGDMT:" + info.hls_para.m3u8_get_delay_avg_time
				+ "|MS:" + info.hls_para.m3u8_server
				+ "|TS:" + info.hls_para.ts_server);

			it_report_unicom.putExtra("VIDEO_ASPECT", info.video_aspect);
			it_report_unicom.putExtra("VIDEO_RATIO", info.video_ratio);
			it_report_unicom.putExtra("VIDEO_FORMAT", info.vformat);
			it_report_unicom.putExtra("VIDEO_SAMPLETYPE", info.progress);
			it_report_unicom.putExtra("AUDIO_FORMAT", info.aformat);
			it_report_unicom.putExtra("AUDIO_BITRATE", info.audio_bitrate);
			it_report_unicom.putExtra("AUDIO_CHANNELS", info.audio_channels);
			it_report_unicom.putExtra("AUDIO_SR", info.audio_sr);
			it_report_unicom.putExtra("SUB_LANGUAGE", info.audio_sub_language);
			it_report_unicom.putExtra("STREAM_TRANSPORT", info.transport_protocol);//default tcp
			it_report_unicom.putExtra("TS_CC_DISCONT", info.ts_cc_discont);
			it_report_unicom.putExtra("TS_SYNC_LOST_NUM", info.ts_sync_lost_num);
			it_report_unicom.putExtra("AVPTS_DIFF", info.avpts_diff);
			it_report_unicom.putExtra("VIDEO_BUF_SIZE", info.vbuf_size);
			it_report_unicom.putExtra("VIDEO_USED_SIZE", info.vbuf_used_size);
			it_report_unicom.putExtra("AUDIO_BUF_SIZE", info.abuf_size);
			it_report_unicom.putExtra("AUDIO_USED_SIZE", info.abuf_used_size);
			it_report_unicom.putExtra("VDEC_ERROR", info.vdec_error);
			it_report_unicom.putExtra("VDEC_DROP", info.vdec_drop);
			it_report_unicom.putExtra("VIDEO_UNDERFLOW", info.vdec_underflow);
			it_report_unicom.putExtra("VIDEO_PTS_ERROR", info.vdec_pts_error);
			it_report_unicom.putExtra("ADEC_ERROR", info.adec_error);
			it_report_unicom.putExtra("ADEC_DROP", info.adec_drop);
			it_report_unicom.putExtra("ADEC_UNDERFLOW", info.adec_underflow);
			it_report_unicom.putExtra("AUDIO_PTS_ERROR", info.adec_pts_error);

			Log.i(TAG, "<vaspect:" + info.video_aspect
				+ "|vratio:" + info.video_ratio
				+ "|vfmt:" + info.vformat
				+ "|vprogress:" + info.progress
				+ "|afmt:" + info.aformat
				+ "|abitrate:" + info.audio_bitrate
				+ "|achn:" + info.audio_channels
				+ "|asr:" + info.audio_sr
				+ "|slan:" + info.audio_sub_language
				+ "|streamproto:" + info.transport_protocol
				+ "|ts_cc_discnt:" + info.ts_cc_discont
				+ "|ts_sync_lost:" + info.ts_sync_lost_num
				+ "|avptsdiff:" + info.avpts_diff
				+ "|vbufsize:" + info.vbuf_size
				+ "|vbufused:" + info.vbuf_used_size
				+ "|abufsize:" + info.abuf_size
				+ "|abufused:" + info.abuf_used_size
				+ "|vdecerr:" + info.vdec_error
				+ "|vdecdrop:" + info.vdec_drop
				+ "|vdecunderflow:" + info.vdec_underflow
				+ "|vptserr:" + info.vdec_pts_error
				+ "|adecerr:" + info.adec_error
				+ "|adecdrop:" + info.adec_drop
				+ "|adecunderflow:" + info.adec_underflow
				+ "|aptserr:" + info.adec_pts_error);
			mContext.sendBroadcast(it_report_unicom);
		}

		return ret;
	}

    /**
    * @hide
    */
    public MediaInfo getMediaInfo() {
    	
        MediaInfo mediaInfo = new MediaInfo();
        Parcel p = Parcel.obtain();
	    try{
            getParameter(KEY_PARAMETER_AML_PLAYER_GET_MEDIA_INFO, p);
	    } catch (Exception e ){
	    mediaInfo = info;
	    Log.i(TAG,"[getMediaInfo error] use info:"+info);
	    p.recycle();
	    return mediaInfo;
        }
        mediaInfo.player_id = p.readInt();
        if (mediaInfo.player_id == 0){
           mediaInfo.player_id++;
        }
        mediaInfo.filename = p.readString();
        mediaInfo.url = p.readString();

        int index = -1;
        if (mediaInfo.url != null && mediaInfo.url.length() > 0) {
            index = mediaInfo.url.indexOf("http");
        }

        if (index > 0){
          mediaInfo.url = mediaInfo.url.substring(index);
        }
        mediaInfo.duration = p.readInt();
        mediaInfo.file_size = p.readString();
        mediaInfo.bitrate = p.readInt();
        mediaInfo.type = p.readInt();
        mediaInfo.fps = p.readInt();
        mediaInfo.cur_video_index = p.readInt();
        mediaInfo.cur_audio_index = p.readInt();
        mediaInfo.cur_sub_index = p.readInt();
        //Log.i(TAG,"[getMediaInfo]filename:"+mediaInfo.filename+",duration:"+mediaInfo.duration+",file_size:"+mediaInfo.file_size+",bitrate:"+mediaInfo.bitrate+",type:"+mediaInfo.type);
        //Log.i(TAG,"[getMediaInfo]cur_video_index:"+mediaInfo.cur_video_index+",cur_audio_index:"+mediaInfo.cur_audio_index+",cur_sub_index:"+mediaInfo.cur_sub_index);

        //----video info----
        mediaInfo.total_video_num = p.readInt();
        //Log.i(TAG,"[getMediaInfo]mediaInfo.total_video_num:"+mediaInfo.total_video_num);
        mediaInfo.videoInfo = new VideoInfo[mediaInfo.total_video_num];
        for (int i=0;i<mediaInfo.total_video_num;i++) {
            mediaInfo.videoInfo[i] = new VideoInfo();
            mediaInfo.videoInfo[i].index = p.readInt();
            mediaInfo.videoInfo[i].id = p.readInt();
            mediaInfo.videoInfo[i].vformat = p.readString();
            mediaInfo.videoInfo[i].width = p.readInt();
            mediaInfo.videoInfo[i].height = p.readInt();
            //Log.i(TAG,"[getMediaInfo]videoInfo i:"+i+",index:"+mediaInfo.videoInfo[i].index+",id:"+mediaInfo.videoInfo[i].id);
            //Log.i(TAG,"[getMediaInfo]videoInfo i:"+i+",vformat:"+mediaInfo.videoInfo[i].vformat);
            //Log.i(TAG,"[getMediaInfo]videoInfo i:"+i+",width:"+mediaInfo.videoInfo[i].width+",height:"+mediaInfo.videoInfo[i].height);
        }

        //----audio info----
        mediaInfo.total_audio_num = p.readInt();
        //Log.i(TAG,"[getMediaInfo]mediaInfo.total_audio_num:"+mediaInfo.total_audio_num);
        mediaInfo.audioInfo = new AudioInfo[mediaInfo.total_audio_num];
        for (int j=0;j<mediaInfo.total_audio_num;j++) {
            mediaInfo.audioInfo[j] = new AudioInfo();
            mediaInfo.audioInfo[j].index = p.readInt();
            mediaInfo.audioInfo[j].id = p.readInt();
            mediaInfo.audioInfo[j].aformat = p.readInt();
            mediaInfo.audioInfo[j].channel = p.readInt();
            mediaInfo.audioInfo[j].sample_rate = p.readInt();
            //Log.i(TAG,"[getMediaInfo]audioInfo j:"+j+",index:"+mediaInfo.audioInfo[j].index+",id:"+mediaInfo.audioInfo[j].id+",aformat:"+mediaInfo.audioInfo[j].aformat);
            //Log.i(TAG,"[getMediaInfo]audioInfo j:"+j+",channel:"+mediaInfo.audioInfo[j].channel+",sample_rate:"+mediaInfo.audioInfo[j].sample_rate);
        }

        //----subtitle info----
        mediaInfo.total_sub_num = p.readInt();
        //Log.i(TAG,"[getMediaInfo]mediaInfo.total_sub_num:"+mediaInfo.total_sub_num);
        mediaInfo.subtitleInfo = new SubtitleInfo[mediaInfo.total_sub_num];
        for (int k=0;k<mediaInfo.total_sub_num;k++) {
            mediaInfo.subtitleInfo[k] = new SubtitleInfo();
            mediaInfo.subtitleInfo[k].index = p.readInt();
            mediaInfo.subtitleInfo[k].id = p.readInt();
            mediaInfo.subtitleInfo[k].sub_type = p.readInt();
            mediaInfo.subtitleInfo[k].sub_language = p.readString();
            //Log.i(TAG,"[getMediaInfo]subtitleInfo k:"+k+",index:"+mediaInfo.subtitleInfo[k].index+",id:"+mediaInfo.subtitleInfo[k].id+",sub_type:"+mediaInfo.subtitleInfo[k].sub_type);
            //Log.i(TAG,"[getMediaInfo]subtitleInfo k:"+k+",sub_language:"+mediaInfo.subtitleInfo[k].sub_language);
        }

        //----ts programe info----
        mediaInfo.total_ts_num = p.readInt();
        //Log.i(TAG,"[getMediaInfo]mediaInfo.total_ts_num:"+mediaInfo.total_ts_num);
        mediaInfo.tsprogrameInfo = new TsProgrameInfo[mediaInfo.total_ts_num];
        for (int l=0;l<mediaInfo.total_ts_num;l++) {
            mediaInfo.tsprogrameInfo[l] = new TsProgrameInfo();
            mediaInfo.tsprogrameInfo[l].v_pid = p.readInt();
            mediaInfo.tsprogrameInfo[l].title = p.readString();
            /*Log.i(TAG,"[getMediaInfo]tsprogrameInfo l:"+l+",v_pid:"+mediaInfo.tsprogrameInfo[l].v_pid+",title:"+mediaInfo.tsprogrameInfo[l].title);
            byte[] data = (mediaInfo.tsprogrameInfo[l].title).getBytes();
            for (int m = 0; m < data.length; m++) {
                Log.i(TAG,"[getMediaInfo]data["+m+"]:"+data[m] + "("+(String.format("0x%x", (0xff & data[m]))) + ")");
            }
            Log.i(TAG,"[getMediaInfo]=======================================");*/
        }

        p.recycle();
	
	//updateId(mediaInfo);

        return mediaInfo;
    }

    private void updateId() {

/* 	 if (info.player_id != lowerPlayerId) {
            lowerPlayerId=info.player_id;
         if (PLAYER_ID_POOL >= PLAYER_ID_POOL_SIZE) {
                PLAYER_ID_POOL=0;
*/
        try {
            if (idFile.exists()) {
                playerId = Integer.valueOf(FileUtils.readTextFile(idFile, 0, null)) + 1;
                if (playerId > PLAYER_ID_POOL_SIZE) {
                    playerId -= PLAYER_ID_POOL_SIZE;
                }
            } else {
                playerId = 1;
            }
            FileUtils.stringToFile(idFilePath, String.valueOf(playerId));
	    idFile.setReadable(true,false);
            idFile.setWritable(true,false);
        } catch (Exception e) {
            playerId = 1;
        }
 //      }
 //           playerId=++PLAYER_ID_POOL;
       // }
    }



    /**
     * Gets the value of the parameter indicated by key.
     * The caller is responsible for recycling the returned parcel.
     * @param key key indicates the parameter to get.
     * @return value of the parameter.
     * {@hide}
     */
    public Parcel getParcelParameter(int key) {
        Parcel p = Parcel.obtain();
        getParameter(key, p);
        return p;
    }

    /**
     * Gets the value of the parameter indicated by key.
     * @param key key indicates the parameter to get.
     * @return value of the parameter.
     * {@hide}
     */
    public String getStringParameter(int key) {
        Parcel p = Parcel.obtain();
        getParameter(key, p);
        String ret = p.readString();
        p.recycle();
        return ret;
    }

    /**
     * Gets the value of the parameter indicated by key.
     * @param key key indicates the parameter to get.
     * @return value of the parameter.
     * {@hide}
     */
    public int getIntParameter(int key) {
        Parcel p = Parcel.obtain();
        getParameter(key, p);
        int ret = p.readInt();
        p.recycle();
        return ret;
    }

    /**
     * Sets the send level of the player to the attached auxiliary effect
     * {@see #attachAuxEffect(int)}. The level value range is 0 to 1.0.
     * <p>By default the send level is 0, so even if an effect is attached to the player
     * this method must be called for the effect to be applied.
     * <p>Note that the passed level value is a raw scalar. UI controls should be scaled
     * logarithmically: the gain applied by audio framework ranges from -72dB to 0dB,
     * so an appropriate conversion from linear UI input x to level is:
     * x == 0 -> level = 0
     * 0 < x <= R -> level = 10^(72*(x-R)/20/R)
     * @param level send level scalar
     */
    public native void setAuxEffectSendLevel(float level);

    /*
     * @param request Parcel destinated to the media player. The
     *                Interface token must be set to the IMediaPlayer
     *                one to be routed correctly through the system.
     * @param reply[out] Parcel that will contain the reply.
     * @return The status code.
     */
    private native final int native_invoke(Parcel request, Parcel reply);


    /*
     * @param update_only If true fetch only the set of metadata that have
     *                    changed since the last invocation of getMetadata.
     *                    The set is built using the unfiltered
     *                    notifications the native player sent to the
     *                    MediaPlayerService during that period of
     *                    time. If false, all the metadatas are considered.
     * @param apply_filter  If true, once the metadata set has been built based on
     *                     the value update_only, the current filter is applied.
     * @param reply[out] On return contains the serialized
     *                   metadata. Valid only if the call was successful.
     * @return The status code.
     */
    private native final boolean native_getMetadata(boolean update_only,
                                                    boolean apply_filter,
                                                    Parcel reply);

    /*
     * @param request Parcel with the 2 serialized lists of allowed
     *                metadata types followed by the one to be
     *                dropped. Each list starts with an integer
     *                indicating the number of metadata type elements.
     * @return The status code.
     */
    private native final int native_setMetadataFilter(Parcel request);

    private static native final void native_init();
    private native final void native_setup(Object mediaplayer_this);
    private native final void native_finalize();

    /**
     * Class for MediaPlayer to return each audio/video/subtitle track's metadata.
     *
     * @see android.media.MediaPlayer#getTrackInfo
     */
    static public class TrackInfo implements Parcelable {
        /**
         * Gets the track type.
         * @return TrackType which indicates if the track is video, audio, timed text.
         */
        public int getTrackType() {
            return mTrackType;
        }

        /**
         * Gets the language code of the track.
         * @return a language code in either way of ISO-639-1 or ISO-639-2.
         * When the language is unknown or could not be determined,
         * ISO-639-2 language code, "und", is returned.
         */
        public String getLanguage() {
            String language = mFormat.getString(MediaFormat.KEY_LANGUAGE);
            return language == null ? "und" : language;
        }

        /**
         * Gets the {@link MediaFormat} of the track.  If the format is
         * unknown or could not be determined, null is returned.
         */
        public MediaFormat getFormat() {
            if (mTrackType == MEDIA_TRACK_TYPE_TIMEDTEXT
                    || mTrackType == MEDIA_TRACK_TYPE_SUBTITLE) {
                return mFormat;
            }
            return null;
        }

        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        /** @hide */
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;

        final int mTrackType;
        final MediaFormat mFormat;

        TrackInfo(Parcel in) {
            mTrackType = in.readInt();
            // TODO: parcel in the full MediaFormat
            String language = in.readString();

            if (mTrackType == MEDIA_TRACK_TYPE_TIMEDTEXT) {
                mFormat = MediaFormat.createSubtitleFormat(
                    MEDIA_MIMETYPE_TEXT_SUBRIP, language);
            } else if (mTrackType == MEDIA_TRACK_TYPE_SUBTITLE) {
                mFormat = MediaFormat.createSubtitleFormat(
                    MEDIA_MIMETYPE_TEXT_VTT, language);
                mFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, in.readInt());
                mFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, in.readInt());
                mFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, in.readInt());
            } else {
                mFormat = new MediaFormat();
                mFormat.setString(MediaFormat.KEY_LANGUAGE, language);
            }
        }

        /** @hide */
        TrackInfo(int type, MediaFormat format) {
            mTrackType = type;
            mFormat = format;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mTrackType);
            dest.writeString(getLanguage());

            if (mTrackType == MEDIA_TRACK_TYPE_SUBTITLE) {
                dest.writeInt(mFormat.getInteger(MediaFormat.KEY_IS_AUTOSELECT));
                dest.writeInt(mFormat.getInteger(MediaFormat.KEY_IS_DEFAULT));
                dest.writeInt(mFormat.getInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE));
            }
        }

        /**
         * Used to read a TrackInfo from a Parcel.
         */
        static final Parcelable.Creator<TrackInfo> CREATOR
                = new Parcelable.Creator<TrackInfo>() {
                    @Override
                    public TrackInfo createFromParcel(Parcel in) {
                        return new TrackInfo(in);
                    }

                    @Override
                    public TrackInfo[] newArray(int size) {
                        return new TrackInfo[size];
                    }
                };

    };

    /**
     * Returns an array of track information.
     *
     * @return Array of track info. The total number of tracks is the array length.
     * Must be called again if an external timed text source has been added after any of the
     * addTimedTextSource methods are called.
     * @throws IllegalStateException if it is called in an invalid state.
     */
    public TrackInfo[] getTrackInfo() throws IllegalStateException {
        TrackInfo trackInfo[] = getInbandTrackInfo();
        // add out-of-band tracks
        TrackInfo allTrackInfo[] = new TrackInfo[trackInfo.length + mOutOfBandSubtitleTracks.size()];
        System.arraycopy(trackInfo, 0, allTrackInfo, 0, trackInfo.length);
        int i = trackInfo.length;
        for (SubtitleTrack track: mOutOfBandSubtitleTracks) {
            allTrackInfo[i] = new TrackInfo(TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE, track.getFormat());
            ++i;
        }
        return allTrackInfo;
    }

    private TrackInfo[] getInbandTrackInfo() throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(INVOKE_ID_GET_TRACK_INFO);
            invoke(request, reply);
            TrackInfo trackInfo[] = reply.createTypedArray(TrackInfo.CREATOR);
            return trackInfo;
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    /* Do not change these values without updating their counterparts
     * in include/media/stagefright/MediaDefs.h and media/libstagefright/MediaDefs.cpp!
     */
    /**
     * MIME type for SubRip (SRT) container. Used in addTimedTextSource APIs.
     */
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";

    /**
     * MIME type for SubOther (sami ass and so on) container. Used in addTimedTextSource APIs.
     * @hide
     */
    public static final String MEDIA_MIMETYPE_TEXT_SUBOTHER = "application/sub-other";

    /**
     * MIME type for WebVTT subtitle data.
     * @hide
     */
    public static final String MEDIA_MIMETYPE_TEXT_VTT = "text/vtt";

    /*
     * A helper function to check if the mime type is supported by media framework.
     */
    private static boolean availableMimeTypeForExternalSource(String mimeType) {
        if (mimeType == MEDIA_MIMETYPE_TEXT_SUBRIP || mimeType == MEDIA_MIMETYPE_TEXT_SUBOTHER) {
            return true;
        }
        return false;
    }

    private SubtitleController mSubtitleController;

    /** @hide */
    public void setSubtitleAnchor(
            SubtitleController controller,
            SubtitleController.Anchor anchor) {
        // TODO: create SubtitleController in MediaPlayer
        mSubtitleController = controller;
        mSubtitleController.setAnchor(anchor);
    }

    private SubtitleTrack[] mInbandSubtitleTracks;
    private int mSelectedSubtitleTrackIndex = -1;
    private Vector<SubtitleTrack> mOutOfBandSubtitleTracks;
    private Vector<InputStream> mOpenSubtitleSources;

    private OnSubtitleDataListener mSubtitleDataListener = new OnSubtitleDataListener() {
        @Override
        public void onSubtitleData(MediaPlayer mp, SubtitleData data) {
            int index = data.getTrackIndex();
            if (index >= mInbandSubtitleTracks.length) {
                return;
            }
            SubtitleTrack track = mInbandSubtitleTracks[index];
            if (track != null) {
                try {
                    long runID = data.getStartTimeUs() + 1;
                    // TODO: move conversion into track
                    track.onData(new String(data.getData(), "UTF-8"), true /* eos */, runID);
                    track.setRunDiscardTimeMs(
                            runID,
                            (data.getStartTimeUs() + data.getDurationUs()) / 1000);
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.w(TAG, "subtitle data for track " + index + " is not UTF-8 encoded: " + e);
                }
            }
        }
    };

    /** @hide */
    @Override
    public void onSubtitleTrackSelected(SubtitleTrack track) {
        if (mSelectedSubtitleTrackIndex >= 0) {
            try {
                selectOrDeselectInbandTrack(mSelectedSubtitleTrackIndex, false);
            } catch (IllegalStateException e) {
            }
            mSelectedSubtitleTrackIndex = -1;
        }
        setOnSubtitleDataListener(null);
        if (track == null) {
            return;
        }
        for (int i = 0; i < mInbandSubtitleTracks.length; i++) {
            if (mInbandSubtitleTracks[i] == track) {
                Log.v(TAG, "Selecting subtitle track " + i);
                mSelectedSubtitleTrackIndex = i;
                try {
                    selectOrDeselectInbandTrack(mSelectedSubtitleTrackIndex, true);
                } catch (IllegalStateException e) {
                }
                setOnSubtitleDataListener(mSubtitleDataListener);
                break;
            }
        }
        // no need to select out-of-band tracks
    }

    /** @hide */
    public void addSubtitleSource(InputStream is, MediaFormat format)
            throws IllegalStateException
    {
        final InputStream fIs = is;
        final MediaFormat fFormat = format;

        // Ensure all input streams are closed.  It is also a handy
        // way to implement timeouts in the future.
        synchronized(mOpenSubtitleSources) {
            mOpenSubtitleSources.add(is);
        }

        // process each subtitle in its own thread
        final HandlerThread thread = new HandlerThread("SubtitleReadThread",
              Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        handler.post(new Runnable() {
            private int addTrack() {
                if (fIs == null || mSubtitleController == null) {
                    return MEDIA_INFO_UNSUPPORTED_SUBTITLE;
                }

                SubtitleTrack track = mSubtitleController.addTrack(fFormat);
                if (track == null) {
                    return MEDIA_INFO_UNSUPPORTED_SUBTITLE;
                }

                // TODO: do the conversion in the subtitle track
                Scanner scanner = new Scanner(fIs, "UTF-8");
                String contents = scanner.useDelimiter("\\A").next();
                synchronized(mOpenSubtitleSources) {
                    mOpenSubtitleSources.remove(fIs);
                }
                scanner.close();
                mOutOfBandSubtitleTracks.add(track);
                track.onData(contents, true /* eos */, ~0 /* runID: keep forever */);
                return MEDIA_INFO_EXTERNAL_METADATA_UPDATE;
            }

            public void run() {
                int res = addTrack();
                if (mEventHandler != null) {
                    Message m = mEventHandler.obtainMessage(MEDIA_INFO, res, 0, null);
                    mEventHandler.sendMessage(m);
                }
                thread.getLooper().quitSafely();
            }
        });
    }

    private void scanInternalSubtitleTracks() {
        if (mSubtitleController == null) {
            Log.e(TAG, "Should have subtitle controller already set");
            return;
        }

        TrackInfo[] tracks = getInbandTrackInfo();
        SubtitleTrack[] inbandTracks = new SubtitleTrack[tracks.length];
        for (int i=0; i < tracks.length; i++) {
            if ((tracks[i] != null) && (tracks[i].getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)) {
                if (i < mInbandSubtitleTracks.length) {
                    inbandTracks[i] = mInbandSubtitleTracks[i];
                } else {
                    SubtitleTrack track = mSubtitleController.addTrack(
                            tracks[i].getFormat());
                    inbandTracks[i] = track;
                }
            }
        }
        mInbandSubtitleTracks = inbandTracks;
        mSubtitleController.selectDefaultTrack();
    }

    /* TODO: Limit the total number of external timed text source to a reasonable number.
     */
    /**
     * Adds an external timed text source file.
     *
     * Currently supported format is SubRip with the file extension .srt, case insensitive.
     * Note that a single external timed text source may contain multiple tracks in it.
     * One can find the total number of available tracks using {@link #getTrackInfo()} to see what
     * additional tracks become available after this method call.
     *
     * @param path The file path of external timed text source file.
     * @param mimeType The mime type of the file. Must be one of the mime types listed above.
     * @throws IOException if the file cannot be accessed or is corrupted.
     * @throws IllegalArgumentException if the mimeType is not supported.
     * @throws IllegalStateException if called in an invalid state.
     */
    public void addTimedTextSource(String path, String mimeType)
            throws IOException, IllegalArgumentException, IllegalStateException {
        if (!availableMimeTypeForExternalSource(mimeType)) {
            final String msg = "Illegal mimeType for timed text source: " + mimeType;
            throw new IllegalArgumentException(msg);
        }

        File file = new File(path);

        //add for subtitle
        mSubPath = path;

        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            FileDescriptor fd = is.getFD();
            addTimedTextSource(fd, mimeType);
            is.close();
        } else {
            // We do not support the case where the path is not a file.
            throw new IOException(path);
        }
    }

    /**
     * Adds an external timed text source file (Uri).
     *
     * Currently supported format is SubRip with the file extension .srt, case insensitive.
     * Note that a single external timed text source may contain multiple tracks in it.
     * One can find the total number of available tracks using {@link #getTrackInfo()} to see what
     * additional tracks become available after this method call.
     *
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @param mimeType The mime type of the file. Must be one of the mime types listed above.
     * @throws IOException if the file cannot be accessed or is corrupted.
     * @throws IllegalArgumentException if the mimeType is not supported.
     * @throws IllegalStateException if called in an invalid state.
     */
    public void addTimedTextSource(Context context, Uri uri, String mimeType)
            throws IOException, IllegalArgumentException, IllegalStateException {
        String scheme = uri.getScheme();
        if(scheme == null || scheme.equals("file")) {

            //add for subtitle
            mSubPath = uri.getPath();

            addTimedTextSource(uri.getPath(), mimeType);
            return;
        }

        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();

            //add for subtitle
            String mediaStorePath = uri.getPath();
            String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA
            };

            if(scheme.equals("content")) {
                int idx_check = (uri.toString()).indexOf("media/external/video/media");
                if(idx_check > -1) {
                    int idx = mediaStorePath.lastIndexOf("/");
                    String idStr = mediaStorePath.substring(idx+1);
                    int id = Integer.parseInt(idStr);
                    if(subtitleServiceDebug()) Log.i(TAG,"[addTimedTextSource]id:"+id);
                    String where = MediaStore.Video.Media._ID + "=" + id;
                    Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cols, where , null, null);
                    if (cursor != null && cursor.getCount() == 1) {
                        int colidx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                        cursor.moveToFirst();
                        mSubPath = cursor.getString(colidx);
                        if(subtitleServiceDebug()) Log.i(TAG,"[addTimedTextSource]mediaStorePath:"+mediaStorePath+",mSubPath:"+mSubPath);
                    }
                }
                else {
                    mSubPath = null;
                }
            }
            else {
                mSubPath = null;
            }

            fd = resolver.openAssetFileDescriptor(uri, "r");
            if (fd == null) {
                return;
            }
            addTimedTextSource(fd.getFileDescriptor(), mimeType);
            return;
        } catch (SecurityException ex) {
        } catch (IOException ex) {
        } finally {
            if (fd != null) {
                fd.close();
            }
        }
    }

    /**
     * Adds an external timed text source file (FileDescriptor).
     *
     * It is the caller's responsibility to close the file descriptor.
     * It is safe to do so as soon as this call returns.
     *
     * Currently supported format is SubRip. Note that a single external timed text source may
     * contain multiple tracks in it. One can find the total number of available tracks
     * using {@link #getTrackInfo()} to see what additional tracks become available
     * after this method call.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @param mimeType The mime type of the file. Must be one of the mime types listed above.
     * @throws IllegalArgumentException if the mimeType is not supported.
     * @throws IllegalStateException if called in an invalid state.
     */
    public void addTimedTextSource(FileDescriptor fd, String mimeType)
            throws IllegalArgumentException, IllegalStateException {
        // intentionally less than LONG_MAX
        addTimedTextSource(fd, 0, 0x7ffffffffffffffL, mimeType);
    }

    /**
     * Adds an external timed text file (FileDescriptor).
     *
     * It is the caller's responsibility to close the file descriptor.
     * It is safe to do so as soon as this call returns.
     *
     * Currently supported format is SubRip. Note that a single external timed text source may
     * contain multiple tracks in it. One can find the total number of available tracks
     * using {@link #getTrackInfo()} to see what additional tracks become available
     * after this method call.
     *
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts, in bytes
     * @param length the length in bytes of the data to be played
     * @param mimeType The mime type of the file. Must be one of the mime types listed above.
     * @throws IllegalArgumentException if the mimeType is not supported.
     * @throws IllegalStateException if called in an invalid state.
     */
    public void addTimedTextSource(FileDescriptor fd, long offset, long length, String mimeType)
            throws IllegalArgumentException, IllegalStateException {
        if (!availableMimeTypeForExternalSource(mimeType)) {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + mimeType);
        }

        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(INVOKE_ID_ADD_EXTERNAL_SOURCE_FD);
            request.writeFileDescriptor(fd);
            request.writeLong(offset);
            request.writeLong(length);
            request.writeString(mimeType);
            if (mSubPath != null) {
                request.writeString(mSubPath);
            }
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    /**
     * Selects a track.
     * <p>
     * If a MediaPlayer is in invalid state, it throws an IllegalStateException exception.
     * If a MediaPlayer is in <em>Started</em> state, the selected track is presented immediately.
     * If a MediaPlayer is not in Started state, it just marks the track to be played.
     * </p>
     * <p>
     * In any valid state, if it is called multiple times on the same type of track (ie. Video,
     * Audio, Timed Text), the most recent one will be chosen.
     * </p>
     * <p>
     * The first audio and video tracks are selected by default if available, even though
     * this method is not called. However, no timed text track will be selected until
     * this function is called.
     * </p>
     * <p>
     * Currently, only timed text tracks or audio tracks can be selected via this method.
     * In addition, the support for selecting an audio track at runtime is pretty limited
     * in that an audio track can only be selected in the <em>Prepared</em> state.
     * </p>
     * @param index the index of the track to be selected. The valid range of the index
     * is 0..total number of track - 1. The total number of tracks as well as the type of
     * each individual track can be found by calling {@link #getTrackInfo()} method.
     * @throws IllegalStateException if called in an invalid state.
     *
     * @see android.media.MediaPlayer#getTrackInfo
     */
    public void selectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, true /* select */);
    }

    /**
     * Deselect a track.
     * <p>
     * Currently, the track must be a timed text track and no audio or video tracks can be
     * deselected. If the timed text track identified by index has not been
     * selected before, it throws an exception.
     * </p>
     * @param index the index of the track to be deselected. The valid range of the index
     * is 0..total number of tracks - 1. The total number of tracks as well as the type of
     * each individual track can be found by calling {@link #getTrackInfo()} method.
     * @throws IllegalStateException if called in an invalid state.
     *
     * @see android.media.MediaPlayer#getTrackInfo
     */
    public void deselectTrack(int index) throws IllegalStateException {
        selectOrDeselectTrack(index, false /* select */);
    }

    private int mAVTrackNum = 0;
    private boolean mTrackGot = false;

    private void selectOrDeselectTrack(int index, boolean select)
            throws IllegalStateException {
        // handle subtitle track through subtitle controller
        SubtitleTrack track = null;
        // add select subtitle
        if (select) {
            if (!mTrackGot) {
                TrackInfo[] trackInfo= getTrackInfo();
                for (int j = 0; j < trackInfo.length; j++) {
                    int trackType = trackInfo[j].getTrackType();
                    if (trackType == TrackInfo.MEDIA_TRACK_TYPE_VIDEO || trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                        mAVTrackNum++;
                    }
                }
                mTrackGot = true;
            }
            int trackIdx = index - mAVTrackNum;
            Log.e(TAG, "Select subtitle trackIdx:"+trackIdx+",index:"+index+",mAVTrackNum:"+mAVTrackNum);
            if (trackIdx >= 0) {
                subtitleOpenIdx(trackIdx);
            }
        }

        if (index < mInbandSubtitleTracks.length) {
            track = mInbandSubtitleTracks[index];
        } else if (index < mInbandSubtitleTracks.length + mOutOfBandSubtitleTracks.size()) {
            track = mOutOfBandSubtitleTracks.get(index - mInbandSubtitleTracks.length);
        }

        if (mSubtitleController != null && track != null) {
            if (select) {
                mSubtitleController.selectTrack(track);
            } else if (mSubtitleController.getSelectedTrack() == track) {
                mSubtitleController.selectTrack(null);
            } else {
                Log.w(TAG, "trying to deselect track that was not selected");
            }
            return;
        }

        selectOrDeselectInbandTrack(index, select);
    }
	
   private static String GetInetAddress(String  host){  
        String IPAddress = "";   
        InetAddress ReturnStr1 = null;  
        try {  
            ReturnStr1 = java.net.InetAddress.getByName(host);  
            IPAddress = ReturnStr1.getHostAddress();  
        } catch (UnknownHostException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
            return  IPAddress;  
        }  
        return IPAddress;  
    }


    private void selectOrDeselectInbandTrack(int index, boolean select)
            throws IllegalStateException {
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeInterfaceToken(IMEDIA_PLAYER);
            request.writeInt(select? INVOKE_ID_SELECT_TRACK: INVOKE_ID_DESELECT_TRACK);
            request.writeInt(index);
            invoke(request, reply);
        } finally {
            request.recycle();
            reply.recycle();
        }
    }


    /**
     * @param reply Parcel with audio/video duration info for battery
                    tracking usage
     * @return The status code.
     * {@hide}
     */
    public native static int native_pullBatteryData(Parcel reply);

    /**
     * Sets the target UDP re-transmit endpoint for the low level player.
     * Generally, the address portion of the endpoint is an IP multicast
     * address, although a unicast address would be equally valid.  When a valid
     * retransmit endpoint has been set, the media player will not decode and
     * render the media presentation locally.  Instead, the player will attempt
     * to re-multiplex its media data using the Android@Home RTP profile and
     * re-transmit to the target endpoint.  Receiver devices (which may be
     * either the same as the transmitting device or different devices) may
     * instantiate, prepare, and start a receiver player using a setDataSource
     * URL of the form...
     *
     * aahRX://&lt;multicastIP&gt;:&lt;port&gt;
     *
     * to receive, decode and render the re-transmitted content.
     *
     * setRetransmitEndpoint may only be called before setDataSource has been
     * called; while the player is in the Idle state.
     *
     * @param endpoint the address and UDP port of the re-transmission target or
     * null if no re-transmission is to be performed.
     * @throws IllegalStateException if it is called in an invalid state
     * @throws IllegalArgumentException if the retransmit endpoint is supplied,
     * but invalid.
     *
     * {@hide} pending API council
     */
    public void setRetransmitEndpoint(InetSocketAddress endpoint)
            throws IllegalStateException, IllegalArgumentException
    {
        String addrString = null;
        int port = 0;

        if (null != endpoint) {
            addrString = endpoint.getAddress().getHostAddress();
            port = endpoint.getPort();
        }

        int ret = native_setRetransmitEndpoint(addrString, port);
        if (ret != 0) {
            throw new IllegalArgumentException("Illegal re-transmit endpoint; native ret " + ret);
        }
    }

    private native final int native_setRetransmitEndpoint(String addrString, int port);

    @Override
    protected void finalize() {
    	if(!is_print_stop_finalize)
	        try {
				synchronized (mSaveLogLock) {
					if(! DEFAULT_LOGFILE.equals(logfile_name)){
						    String mMsg = new String(" call stop\r\n destory this player\r\n");
							byte log_buf[] = mMsg.getBytes();
							Log.e(TAG, "mMsg: " + mMsg);
							File fp = new File(RECORD_PATH + File.separator + logfile_name);
							OutputStream out = null;
							out = new FileOutputStream(fp,true);
							for (int i = 0; i < log_buf.length; i++){
								out.write(log_buf[i]);
							}
							out.close();
							mMsg = "";
							//Runtime.getRuntime().exec("chmod 666 " + RECORD_PATH+File.separator+logfile_name);
							is_print_stop_finalize = true;
					}
		        }
			} catch (Exception e ){
				    e.printStackTrace();
			}
		native_finalize(); 
	}

    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    private static final int MEDIA_NOP = 0; // interface test message
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_STARTED = 6;
    private static final int MEDIA_PAUSED = 7;
    private static final int MEDIA_STOPPED = 8;
    private static final int MEDIA_SKIPPED = 9;
    private static final int MEDIA_RESUME = 10;
    private static final int MEDIA_SEEK_START = 11;
    private static final int MEDIA_EXIT = 12;
    private static final int MEDIA_PRELOAD = 13;

    /*
      *  cmcc msg report
      */
    private static final int MEDIA_BLURREDSCREEN_START = 14;
    private static final int MEDIA_BLURREDSCREEN_END = 15;
    private static final int MEDIA_UNLOAD_START = 16;
    private static final int MEDIA_UNLOAD_END = 17;
    private static final int MEDIA_INFO_AMLOGIC_VIDEO_NOT_SUPPORT = 8001;
    private static final int MEDIA_INFO_AMLOGIC_AUDIO_NOT_SUPPORT = 8002;
    private static final int MEDIA_INFO_AMLOGIC_NO_VIDEO = 8003;
    private static final int MEDIA_INFO_AMLOGIC_NO_AUDIO = 8004;
    private static final int MEDIA_ERROR_PLAYER_REPORT = 301;
    /*
      * end
      */

    private static final int MEDIA_TIMED_TEXT = 99;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;
    private static final int MEDIA_SUBTITLE_DATA = 201;
    private static final int MEDIA_BLURAY_INFO = 202;
    private static final int MEDIA_BITRATE_CHANGE = 203;
    private static final int MEDIA_GET_FIRST_PCR = 204;
    private static final int MEDIA_SET_DATASOURCE = 205;
    private static final int MEDIA_AML_SUBTITLE_START = 800; // random value
	private static final int MEDIA_INFO_REPORT = 900;
	private static final int PRINT_LOG_SETPLAYER_SOURCE= 300;
	private static final int PRINT_LOG_CREATE_PLAYER = 301;
	private static final int PRINT_LOG_SET_VIDEOSURFACETEXTURE = 302;
	private static final int PRINT_LOG_PREPAREASYNC = 303;
	private static final int PRINT_LOG_PLAYMODE = 304;
	private static final int PRINT_LOG_FRAME_SIZE = 305;
	private static final int PRINT_LOG_CODEC_INFO = 306;
	private static final int PRINT_LOG_START_DECODER = 307;
	private static final int PRINT_LOG_PREPARED = 308;
	private static final int PRINT_LOG_START = 309;
	private static final int PRINT_LOG_FIRST_FRMAE_SHOWN = 310;
	private static final int PRINT_LOG_PAUSE = 311;
	private static final int PRINT_LOG_SEEKTO = 312;
	private static final int PRINT_LOG_SEEK_COMPLETE = 313;
	private static final int PRINT_LOG_STOP = 314;
	private static final int PRINT_LOG_DESTORY_PLAYER = 315;
	private static final int PRINT_LOG_CONNECT_SERVER = 316;
	private static final int PRINT_LOG_STATUS_CODE = 317;
	private static final int PRINT_LOG_REDIRECT_URL = 318;
	private static final int PRINT_LOG_TS_DOWNLOAD_BAGIN = 319;
	private static final int PRINT_LOG_TS_DOWNLOAD_COMPLETE = 320;
	private static final int PRINT_LOG_CHANNEL_SWITCH = 321;
	private static final int PRINT_LOG_BUFFER_START = 322;
	private static final int PRINT_LOG_BUFFER_FINISH = 323;
	private static final int PRINT_LOG_CURRENT_PLAYTIME = 324;
	private static final int PRINT_LOG_ERROR = 325;
	private static final int PRINT_LOG_pLAY_END = 326;
	private static final int MEDIA_INFO_AMLOGIC_VIDEO_CODEC = 710;
	private static final int MEDIA_INFO_AMLOGIC_AUDIO_CODEC = 711;
    private static final String[] player_error = {
		"PLAYER_PTS_ERROR",
		"PLAYER_NO_DECODER",
		"DECODER_RESET_FAILED",
		"DECODER_INIT_FAILED",
		"PLAYER_UNSUPPORT",
		"PLAYER_UNSUPPORT_VIDEO",
		"PLAYER_UNSUPPORT_AUDIO",
		"PLAYER_SEEK_OVERSPILL",
		"PLAYER_CHECK_CODEC_ERROR",
		"PLAYER_INVALID_CMD",
		"PLAYER_REAL_AUDIO_FAILED",
		"PLAYER_ADTS_NOIDX",
		"PLAYER_SEEK_FAILED",
		"PLAYER_NO_VIDEO",
		"PLAYER_NO_AUDIO",
		"PLAYER_SET_NOVIDEO",
		"PLAYER_SET_NOAUDIO",
		"PLAYER_FFFB_UNSUPPORT",
		"PLAYER_UNSUPPORT_VCODEC",
		"PLAYER_UNSUPPORT_ACODEC",
	};


    private TimeProvider mTimeProvider;

    /** @hide */
    public MediaTimeProvider getMediaTimeProvider() {
        if (mTimeProvider == null) {
            mTimeProvider = new TimeProvider(this);
        }
        return mTimeProvider;
    }
    private static final int SYS_REETRY_TIME = 3;



    static final String RECORD_PATH = "/tmp/playInfoLog/";
    static final String RECORD_FILE_SUFFIX = ".txt";
    static final String RECORD_FILE_SYSTIME_PRE = "#systemtime:";
    static final String RECORD_FILE_URL_PRE = "url:";
    static final String RECORD_FILE_PLAYTIME_PRE = "playtime:";
    static final String RECORD_FILE_STATUS_PRE = "status:";
    static final String RECORD_FILE_DESC_PRE = "!!!desc:";
	static final String DEFAULT_LOGFILE = "20140725111701.txt";
    static String logfile_name = DEFAULT_LOGFILE;
	static final SimpleDateFormat gcur_timeinfo = new SimpleDateFormat("yyyyMMddHHmmss");
    static String tmp_buffing_start_buf = "";//store 701
    static String pre_sys_time = "20140725111702";
    static final int RECORD_TIME_INTERVAL = (15*60*1000);
	static int mFirstTime_interval = RECORD_TIME_INTERVAL;
    static int record_time = 0;
    static boolean b_first_time_symbol = true;
	static final String RECORD_FILE_BEGIN = " ";
	static boolean is_print_stop_finalize = false;
     int START = 500;   
     
    int END = 15000;  

    private class EventHandler extends Handler
    {
        private MediaPlayer mMediaPlayer;

        public EventHandler(MediaPlayer mp, Looper looper) {
            super(looper);
            mMediaPlayer = mp;
        }

        @Override
        public void handleMessage(Message msg) {
			try{
            if (mMediaPlayer.mNativeContext == 0) {
                Log.w(TAG, "mediaplayer went away with unhandled events");
                return;
            }
            //Log.e(TAG, "*****handleMessage :   " + msg);
            if ("mobile".equals(SystemProperties.get("sys.proj.type"))) {
                Message mMesg = Message.obtain();
                mMesg.copyFrom(msg);
                if (mSaveLogThread != null && mSaveLogThread.mHandler != null) {
                    mSaveLogThread.mHandler.sendMessage(mMesg);
                } else {
                    Log.e(TAG, "mSaveLogThread=" + mSaveLogThread + ", mHandler=" + mSaveLogThread.mHandler);
                }
            }
            //these messages are added for save logs,no need to process as below
            if (msg.what == MEDIA_INFO) {
                if (msg.arg1 == MEDIA_INFO_HTTP_CONNECT_OK ||
                        msg.arg1 == MEDIA_INFO_HTTP_CONNECT_ERROR ||
                        msg.arg1 == MEDIA_INFO_HTTP_CODE ||
                        msg.arg1 == MEDIA_INFO_HTTP_REDIRECT ||
                        msg.arg1 == MEDIA_INFO_LIVE_SHIFT )
                    return;
            }

            int player_id = 0;
            cmd = null;
            if(mCmccPlayer){
                cmd = new Intent("MEDIA_PLAY_UNUSED");
            }else{
                cmd = new Intent("MEDIA_PLAY_MONITOR_MESSAGE");
            }
            Log.v(TAG,"msg.what"+msg.what);
            switch(msg.what) {
                case MEDIA_EXIT:
				    Log.e(TAG, "MEDIA_EXIT,mSendPrepareEvent: " + mSendPrepareEvent + "mSendQuitEvent: " + mSendQuitEvent);
					Log.e(TAG, "MEDIA_EXIT");
                    if(SystemProperties.getBoolean("media.player.report.udp", false)&& mSendPrepareEvent && !mSendQuitEvent){
                        long time = System.currentTimeMillis()/1000;
                        if(null == info)
                            info= getMediaInfo();
                        StringBuffer buf=new StringBuffer();
                        buf.append("<type>"+"Stop"+"</type>");
                        buf.append('\n');
                        buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                        buf.append('\n');
                        buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                        sendDataReport(buf.toString(), buf.length());
                        mSendQuitEvent = true;
                        Log.e(TAG, buf.length()+" ,:"+buf.toString());
                    }
                    else if(mContext != null && mSendPrepareEvent && !mSendQuitEvent){
                        int starttime = getCurrentPosition();
                        Log.d(TAG , "starttime is : "+starttime/1000);
                        cmd.putExtra("TYPE", "PLAY_QUIT");
			 mSeekFlag=false;
			 mPlayStartFlag=false; 
                        cmd.putExtra("PLAY_TIME", starttime/1000);
                        cmd.putExtra("TIME", System.currentTimeMillis());
                        if(null == info)
                            info= getMediaInfo();
                        cmd.putExtra("ID", playerId);
                        Log.d(TAG , "amPlayer id is : "+info.player_id);
                        Log.d(TAG , "player id is : "+playerId);
                        //begin:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
                        if(!mbIsFileDescriptor){
			    if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
					|| SystemProperties.get("ro.ysten.province","master").contains("jiangsu")
					|| SystemProperties.get("ro.ysten.province","master").contains("anhui")
					|| SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")
                    || SystemProperties.get("ro.ysten.province","master").contains("CM201_guangdong_zhuoying")
                    || SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
				if(!mQuitFlag){
                                    mContext.sendBroadcast(cmd);
                                    mSendQuitEvent = true;
                                    mQuitFlag=true;
				}
                            }else{
                                mContext.sendBroadcast(cmd);
                                mSendQuitEvent = true;						
			    }
			//add by ysten-mark for heilongjiang sqm playend
			 if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                onEvent(HuaweiUtils.ACTION_PLAYEND,  HuaweiUtils.PARAM_ENDREASON + "=" + HuaweiUtils.ENDREASON_CLOSED); 
                close();
             } 
			//add by ysten-mark for heilongjiang sqm playend
			}
                        //end:add by zhanghk at 20190524:fix report two PLAY_QUIT problem
                    }
                return;
            case MEDIA_PRELOAD:
                long current_time = System.currentTimeMillis();
                if (mReportTimeOffset == 0) {
                    mReportTimeOffset = current_time-mSetDataSourceTime-10000;
                    Log.e(TAG, "------------PLAYABE_REPORT  offset=" + mReportTimeOffset);
                } else {
                    long time_offset = current_time-mLastReportTime-10000;
                    if (Math.abs(time_offset) >= 500) {
                        mReportTimeOffset += time_offset;
                        Log.e(TAG, "------------PLAYABE_REPORT  abs=" + time_offset);
                        Log.e(TAG, "------------PLAYABE_REPORT  offset =" + mReportTimeOffset);
                    }
                }
                if(SystemProperties.getBoolean("media.player.report.udp", false)){
                    int starttime = getCurrentPosition();
                    long time = current_time/1000;
                    if(null == info)
                        info= getMediaInfo();
                    StringBuffer buf=new StringBuffer();
                    buf.append("<type>"+"CacheBytes"+"</type>");
                    buf.append('\n');
                    buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                    buf.append('\n');
                    buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                    buf.append('\n');
                    buf.append("<bytes>"+String.valueOf(msg.arg2)+"</bytes>");
                    buf.append('\n');
                    buf.append("<bitrate>"+String.valueOf(info.bitrate)+"</bitrate>");
                    buf.append('\n');
                    buf.append("<playedtime>"+String.valueOf(starttime/1000)+"</playedtime>");
                    sendDataReport(buf.toString(), buf.length());
                    Log.e(TAG, buf.length()+" ,:"+buf.toString());	
                }else if(mContext != null){
                    int starttime = (int)(getCurrentPosition()/1000);
                    cmd.putExtra("TYPE", "PLAYABE_REPORT");
			long mSecond = (long)msg.arg1;
			if (mBufferStartFlag)
			{
				if ( mSecond > 3 )
				{
					mSecond = 3;
				}
			}
                    cmd.putExtra("SECONDS", mSecond);
		    Log.d(TAG, "MEDIA_PRELOAD  SECONDS=" + (long)msg.arg1);
                    mLastReportTime = current_time;
                    cmd.putExtra("TIME", current_time/*-mReportTimeOffset*/);
                    if(null == info)
                        info= getMediaInfo();
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("PLAY_TIME", starttime);
		    Log.w(TAG, "MEDIA_PRELOAD: starttime=" + starttime);
                    if (msg.obj instanceof Parcel) {
                        Parcel parcel = (Parcel) msg.obj;
                        int pre_fec_ratio = parcel.readInt();
                        int after_fec_ratio = parcel.readInt();
			        int cached_bytes = parcel.readInt();
                        parcel.recycle();
                        Log.e(TAG, "prefec_ratio" + pre_fec_ratio + ",after_fec_ratio" + after_fec_ratio + ",cached_bytes" + cached_bytes);
                        cmd.putExtra("PRE_FEC", pre_fec_ratio);
                        cmd.putExtra("AFTER_FEC", after_fec_ratio);
			        cmd.putExtra("BYTES", cached_bytes);
                    }
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                    }
                }
                return;
            case MEDIA_BLURREDSCREEN_START:
                Log.w(TAG, "media event: BLURREDSCREEN START\n");
                //add by chenfeng at 20191005:limit MEDIA_BLURREDSCREEN_START
                if(SystemProperties.get("ro.ysten.province","master").contains("guangdong")) {
                    if(mBlurredscreenStartCnt == 0) {
                        mEventHandler.postDelayed(blurredscreenStartEvent,2*60*60*1000);
                    }
                    int cnt = SystemProperties.getInt("sys.blur.cnt.limit",-1);
                    Log.w(TAG, "sys.blur.cnt.limit = " + cnt);
                    if(cnt >= 0 && mBlurredscreenStartCnt >= cnt) {
                        Log.w(TAG, "mBlurredscreenStartCnt = " + mBlurredscreenStartCnt);
                        return;
                    }
                    
                }
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "BLURREDSCREEN_START");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("START_TIME", System.currentTimeMillis());
                    cmd.putExtra("PLAY_TIME", starttime/1000);
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
			//begin by ysten.zhangjunjian for blurred end event 
			SystemProperties.set("sys.yst.mblurredscreenstatus", "0");
                        mBlurredscreenStartSend = true;
                        mBlurredscreenStartCnt += 1;
                        Log.w(TAG, "media event: BLURREDSCREEN START mBlurredscreenStartCnt = " + mBlurredscreenStartCnt);
                        //int index=(int)(Math.random()*arr.length);
                        Random rand = new Random();
			            int number = rand.nextInt(END-START+1) + START;
		               Log.d("zjj","huapingshichang"+number);
                       if(SystemProperties.get("ro.ysten.province","master").contains("guangdong")){
                       mEventHandler.postDelayed(blurredscreenStartTimeEvent,number);
                       }
		       //end by ysten.zhangjunjian for blurred end event
                    }
                }
                return;
            case MEDIA_BLURREDSCREEN_END:
                Log.w(TAG, "media event: BLURREDSCREEN END\n");
                if(mContext != null && mBlurredscreenStartSend) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "BLURREDSCREEN_END");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
                    cmd.putExtra("RATIO", msg.arg1);
                    if((!mbIsFileDescriptor)&& SystemProperties.get("sys.yst.mblurredscreenstatus", "1").equals("0")){
                        mContext.sendBroadcast(cmd);
			//begin ysten.zhangjunjian,20191012,for blurred symbolic
                        SystemProperties.set("sys.yst.mblurredscreenstatus", "1");
                        mBlurredscreenStartSend = false;
                       //end ysten.zhangjunjian,20191012,for blurred symbolic
                    }
                }
                return;
            case MEDIA_UNLOAD_START:
                Log.w(TAG, "media event: UNLOAD START\n");
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "UNLOAD_START");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("START_TIME", System.currentTimeMillis());
                    cmd.putExtra("PLAY_TIME", starttime/1000);
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
			//begin ysten.zhangjunjian,20191012,for unload symbolic
                        Log.d(TAG,"2222");
                        SystemProperties.set("sys.yst.munloadstatus", "0");
			mUnloadStartSend = true;
                        Random rand = new Random();
                         int number2 = rand.nextInt(END-START+1) + START;
                         Log.d("zjj","unload"+number2);
                        if(SystemProperties.get("ro.ysten.province","master").contains("guangdong")){
                         mEventHandler.postDelayed(unloadStartTimeEvent,number2);
                        }
		        //end ysten.zhangjunjian,20191012,for unload symbolic
                    }
                }
                return;
            case MEDIA_UNLOAD_END:
                Log.w(TAG, "media event: UNLOAD END\n");
                if(mContext != null) {
                    int starttime = getCurrentPosition();
                    if(null == info)
                        info = getMediaInfo();
                    cmd.putExtra("TYPE", "UNLOAD_END");
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
                    if((!mbIsFileDescriptor)&& SystemProperties.get("sys.yst.munloadstatus", "1").equals("0")){
                        Log.d(TAG,"END222");
                        mContext.sendBroadcast(cmd);
			//begin ysten.zhangjunjian,20191012,for unload symbolic
                        SystemProperties.set("sys.yst.munloadstatus", "1");	
			mUnloadStartSend = false;
			//end ysten.zhangjunjian,20191012,for unload symbolic  
                    }
                }
                return;

            case MEDIA_SEEK_START:
                Log.w(TAG, "media_evt:SEEK_START");
                if(mContext != null){
                    //int starttime = getCurrentPosition();
                    cmd.putExtra("TYPE", "SEEK_START");
	             mSeekFlag=true;
                    cmd.putExtra("PLAY_TIME", mSeekTime/1000);
                    cmd.putExtra("START_TIME", System.currentTimeMillis());
                    if(null == info)
                       info= getMediaInfo();
                    cmd.putExtra("ID", playerId);
                    if(!mbIsFileDescriptor){
                    	if(SystemProperties.get("ro.ysten.province").contains("liaoning") && mBufferingStartSend ){
				Log.d("ystelk","Seek start ....");
                        	mBufferingStartSend = false;
                        	cmdBuffer = cmd;
                        	cmdBuffer.putExtra("TYPE", "BUFFER_END");
                        	cmdBuffer.putExtra("END_TIME", System.currentTimeMillis());
                        	mContext.sendBroadcast(cmdBuffer);
				int seekStartTime = getCurrentPosition();
				cmd.putExtra("TYPE","SEEK_START");
				cmd.putExtra("PLAY_TIME",mSeekTime/1000);
				cmd.putExtra("START_TIME",System.currentTimeMillis());
				cmd.putExtra("ID",playerId);
				mContext.sendBroadcast(cmd);
				mSeekStartSend = true;
                     	}else {
                       		mContext.sendBroadcast(cmd);
		               	mSeekStartSend = true;
			}
                    }
			//add by ysten-mark for heilongjiang sqm seekstart
				   if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                      int position = getCurrentPosition()/1000;
                      onEvent(HuaweiUtils.ACTION_SEEKSTART, HuaweiUtils.PARAM_POSITION + "=" + position);
                    }
/*		    if(SystemProperties.get("ro.ysten.province").contains("liaoning") && mBufferStartFlag ){
                    	int endBufferTime = getCurrentPosition();
			mBufferStartFlag = false;
			cmdBuffer = cmd;
			cmdBuffer.putExtra("TYPE", "BUFFER_END");
                        cmdBuffer.putExtra("END_TIME", endBufferTime);
			
			
		     }
*/
			//add by ysten-mark for heilongjiang sqm seekstart
                } else {
                	mSeekNeedResend = true; // context null. cache seek start will send in setDisplay
                }
                return;
            case MEDIA_RESUME:
                if(SystemProperties.getBoolean("media.player.report.udp", false)){
                    int starttime = getCurrentPosition();
                    long time = System.currentTimeMillis()/1000;
                    if(null == info)
                        info= getMediaInfo();
                    StringBuffer buf=new StringBuffer();
                    buf.append("<type>"+"Resume"+"</type>");
                    buf.append('\n');
                    buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                    buf.append('\n');
                    buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                    buf.append('\n');	
                    buf.append("<resumeseconds>"+String.valueOf((float)starttime/1000)+"</resumeseconds>");
                    buf.append('\n');
                    buf.append("<url>"+info.url+"</url>");
                    buf.append('\n');
                    buf.append("<programtype>"+String.valueOf(info.type)+"</programtype>");
                    sendDataReport(buf.toString(), buf.length());
                    Log.e(TAG, buf.length()+" ,:"+buf.toString());
                }
                else if(mContext != null && mPauseFlag){
			        mPauseFlag = false;
                    int starttime = getCurrentPosition();
                    cmd.putExtra("TYPE", "RESUME_MESSAGE");
                    cmd.putExtra("PLAY_TIME", starttime/1000);
                    cmd.putExtra("TIME", System.currentTimeMillis());
                    if(null == info)
                        info= getMediaInfo();
                    if(!mbIsFileDescriptor){
                    cmd.putExtra("ID", playerId);
                    mContext.sendBroadcast(cmd);
                    }
					//add by ysten-mark for heilongjiang sqm resume
					if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                    int position = getCurrentPosition()/1000;
                    onEvent(HuaweiUtils.ACTION_RESUME, HuaweiUtils.PARAM_POSITION + "=" + position);
//				+ "&" + HuaweiUtils.PARAM_VIDEO_SIZE + "=" + videoSize); 
                    }
					//end add by ysten-mark for heilongjiang sqm resume
                }
                return;
            case MEDIA_STARTED:
                if(mContext != null) {
                     long start = System.currentTimeMillis();
                     cmd.putExtra("TYPE", "PLAY_STARTUP");
                     if(null == info) {
                        info = getMediaInfo();
		     }
		     //add by zhanghk at 20181111 start:fix STARTUP ID is 0 problem
                    	cmd.putExtra("ID", playerId);
			cmd.putExtra("TIME", start);

                        if ((SystemProperties.get("ro.ysten.province","master").contains("liaoning")
                            || SystemProperties.get("ro.ysten.province","master").contains("CM201_guangdong_zhuoying")) && mSeekStartSend){
                                mSeekStartSend =false;
                                cmdSeek = cmd;
				cmdSeek.putExtra("ID",playerId);
				cmdSeek.putExtra("TIME",mSeekTime/1000);
                                cmdSeek.putExtra("TYPE","SEEK_END");
                  		cmd.putExtra("END_TIME",System.currentTimeMillis());
                                mContext.sendBroadcast(cmdSeek);
                     		long startAgain = System.currentTimeMillis();
                    	   	cmd.putExtra("ID", playerId);
				cmd.putExtra("TIME",startAgain);
                                cmd.putExtra("TYPE","PLAY_STARTUP");
                                mContext.sendBroadcast(cmd);
                        } else if(!mbIsFileDescriptor){
				mContext.sendBroadcast(cmd);
                    }
		}
                return;
            case MEDIA_GET_FIRST_PCR:
                if(SystemProperties.getBoolean("media.player.report.udp", false)){
                    int starttime = getCurrentPosition();
                    long time = System.currentTimeMillis()/1000;
                    if(null == info)
                        info= getMediaInfo();
                    StringBuffer buf=new StringBuffer();
                    buf.append("<type>"+"Start"+"</type>");
                    buf.append('\n');
                    buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                    buf.append('\n');
                    buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                    buf.append('\n');		
                    buf.append("<bufferseconds>"+String.valueOf((float)(time-mSetDataSourceTime)/1000)+"</bufferseconds>");
                    buf.append('\n');
                    buf.append("<url>"+info.url+"</url>");
                    buf.append('\n');
                    buf.append("<programtype>"+String.valueOf(info.type)+"</programtype>");
                    sendDataReport(buf.toString(), buf.length());
                    Log.e(TAG, buf.length()+" ,:"+buf.toString());
                }
                else if(mContext != null){
                    long start = System.currentTimeMillis();
                    int starttime = getCurrentPosition();
                    cmd.putExtra("TYPE", "PLAY_START");
                    cmd.putExtra("PLAY_TIME", starttime/1000);
                    cmd.putExtra("END_TIME", System.currentTimeMillis());
		    if(null == info){
                    	info = getMediaInfo();
		    }
                    if ((info != null) && (info.total_video_num > 0)) {
                        cmd.putExtra("ID", playerId);
                        cmd.putExtra("BITRATE", info.bitrate);
                        cmd.putExtra("FRAMERATE",info.fps);
                        Log.w(TAG, "framerate:"+info.fps);
                        Log.w(TAG, "width"+info.videoInfo[0].width+"height"+info.videoInfo[0].height);
                        cmd.putExtra("WIDTH", info.videoInfo[0].width);
                        cmd.putExtra("HEIGHT",info.videoInfo[0].height);
					}
                    //begin:add by zhanghk at 20190525:for report many PLAY_START problem
                    if(!mbIsFileDescriptor){
			if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
				|| SystemProperties.get("ro.ysten.province","master").contains("jiangsu")
				|| SystemProperties.get("ro.ysten.province","master").contains("anhui")
				|| SystemProperties.get("ro.ysten.province","master").contains("A20_sc")
				|| SystemProperties.get("ro.ysten.province", "master").contains("heilongjiang")
                		|| SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
			    if(!mPlayStartFlag){
                                mContext.sendBroadcast(cmd);
				mPlayStartFlag=true;
			    }
			}else if (SystemProperties.get("ro.ysten.province","master").contains("liaoning")&& mSeekStartSend){
				mSeekStartSend =false;
				cmdSeek = cmd;
                                cmdSeek.putExtra("ID",playerId);
                                cmdSeek.putExtra("TIME",mSeekTime/1000);
                                cmdSeek.putExtra("TYPE","SEEK_END");
                                cmd.putExtra("END_TIME",System.currentTimeMillis());
				mContext.sendBroadcast(cmdSeek);
                                cmdSeek.putExtra("ID",playerId);
				cmd.putExtra("TYPE","PLAY_START");
                    		cmd.putExtra("PLAY_TIME", starttime/1000);
                    		cmd.putExtra("END_TIME", System.currentTimeMillis());
			    	mContext.sendBroadcast(cmd);
				Log.w(TAG,"cmd.type = "+ cmd.getExtra("TYPE"));
			} else {
			    mContext.sendBroadcast(cmd);
			}
                    }
                    //end:add by zhanghk at 20190525:for report many PLAY_START problem
				if(SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")){
					//getIPPort();
					open("VOD", "TEST", info.url);
					String mm_url = info.url;
					if(mm_url.startsWith("http:/") || mm_url.startsWith("rtsp:/")|| mm_url.startsWith( "rtp:/")
					|| mm_url.startsWith("mms:/")|| mm_url.startsWith("https:/")|| mm_url.startsWith("rtmp:/")
					|| mm_url.startsWith( "udp:/")){
						try {
							URL urlURL = new URL(mm_url);
						    mstrServerIP = urlURL.getHost();
						    mServerPort = urlURL.getPort();
							if (mServerPort == -1)
							{
								mServerPort = 8080;
							}
						} catch (Exception e ){
						    e.printStackTrace();
					    }
					}
						    //Log.d("mark", "urlURL getPort = " + urlURL.getPort());
					if(!isIP(mstrServerIP) && !TextUtils.isEmpty(mstrServerIP))
                    {
						Thread thread = null;
                		thread = new Thread(new Runnable() {
							@Override
                			public void run() {
                                mstrServerIP = GetInetAddress(mstrServerIP);
								onEvent(HuaweiUtils.ACTION_PLAYSTART, HuaweiUtils.PARAM_SERVERIP + "=" + mstrServerIP 
									+ "&" + HuaweiUtils.PARAM_SERVERPORT + "=" + mServerPort
									+ "&" + HuaweiUtils.PARAM_BITRATE+ "=" + info.bitrate
									+ "&" + HuaweiUtils.PARAM_DURATION+ "=" + info.duration
									+ "&" + HuaweiUtils.PARAM_VIDEO_SIZE + "=" + info.file_size);
								Log.d("mark","mstrServerIP = "+ mstrServerIP);
								Log.d("mark","mServerPort = "+ mServerPort);
							}
						});
						thread.start();	
					} 
					else {
						mstrServerIP = "127.0.0.1";	
					}
				}
                }
                return;
            case MEDIA_BITRATE_CHANGE:
                if(SystemProperties.getBoolean("media.player.report.udp", false)){
                    int starttime = getCurrentPosition();
                    long time = System.currentTimeMillis()/1000;
                    if(null == info)
                        info= getMediaInfo();
                    StringBuffer buf=new StringBuffer();
                    buf.append("<type>"+"BitrateChange"+"</type>");
                    buf.append('\n');
                    buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                    buf.append('\n');
                    buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                    buf.append('\n');
                    buf.append("<tocontentid>"+String.valueOf(playerId)+"</tocontentid>");
                    sendDataReport(buf.toString(), buf.length());
                    Log.e(TAG, buf.length()+" ,:"+buf.toString());	
                }else if(mContext != null){
                    cmd.putExtra("TYPE", "BITRATE_CHANGE");
                    cmd.putExtra("TIME", System.currentTimeMillis());
                    if(null == info)
                        info= getMediaInfo();
                    cmd.putExtra("OLDID", playerId);
                    cmd.putExtra("ID", playerId);
                    Parcel parcel = (Parcel) msg.obj;
                    String new_url = new String(" ");
                    new_url = parcel.readString();
                    cmd.putExtra("TO_URL", new_url);
                    parcel.recycle();

                    cmd.putExtra("TO_BITRATE", msg.arg1);
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                    }
                }
                return;
            case MEDIA_PREPARED:
                Log.v(TAG,"dbg:// MEDIA_PREPARED" +"playerId" + playerId );
                if(300 != msg.arg1)
                	scanInternalSubtitleTracks();
                if (mOnPreparedListener != null)
                    mOnPreparedListener.onPrepared(mMediaPlayer);
                subtitleStart();
              //  if (mContext != null){
	   	    long start = System.currentTimeMillis();
		    cmd.putExtra("TYPE", "PREPARE_COMPLETED");
		    if (null == info){
			info = getMediaInfo();
		    }
                    cmd.putExtra("ID", playerId);
                    cmd.putExtra("TIME", start);
			
                    if(!mbIsFileDescriptor){
                         sendBroadCastPrivate(cmd);

		//	mContext.sendBroadcast(cmd);
                    }
		//}

                //return;PD#140353 should send set datasource sendBroadcast before other.
            case MEDIA_SET_DATASOURCE:
	        Log.e(TAG, "dbg:// MEDIA_SET_DATASOURCE" +"playerId" + playerId  +"mSendPrepareEvent" + mSendPrepareEvent );	
			
                if(SystemProperties.getBoolean("media.player.report.udp", false)&& !mSendPrepareEvent){
                    int starttime = getCurrentPosition();
                    long time = System.currentTimeMillis()/1000;
                    if(null == info)
                        info= getMediaInfo();
                    StringBuffer buf=new StringBuffer();
                    buf.append("<type>"+"Prepare"+"</type>");
                    buf.append('\n');
                    buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                    buf.append('\n');
                    buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                    buf.append('\n');
                    buf.append("<url>"+info.url+"</url>");
                    sendDataReport(buf.toString(), buf.length());
                    mSendPrepareEvent = true;
                    mSendQuitEvent = false;

                    Log.e(TAG, buf.length()+" ,:"+buf.toString());	
                }else if(!mSendPrepareEvent){
                    long start1 = System.currentTimeMillis();
					
                     cmd.putExtra("TYPE", "PLAY_PREPARE");
		     mSeekFlag=false;
                     cmd.putExtra("START_TIME", start1);

                        cmd.putExtra("ID", playerId);
                        Log.d(TAG , "url is : " + (String) msg.obj);
                        cmd.putExtra("URL", (String) msg.obj);

                    if(!mbIsFileDescriptor){
                        sendBroadCastPrivate(cmd);
                        mSendPrepareEvent = true;
		         Log.e(TAG, "dbg://5524 mSendPrepareEvent:"+mSendPrepareEvent);								
                        mSendQuitEvent = false;
                        //add by chenfeng at 20191005:fix MEDIA_BLURREDSCREEN_END may not send
                        mBufferingStartSend = false;
                        mBlurredscreenStartSend = false;
                        mUnloadStartSend = false;
                        mBlurredscreenStartCnt = 0;

                    }
                }
                return;
            case MEDIA_PLAYBACK_COMPLETE:
                if (mOnCompletionListener != null)
                    mOnCompletionListener.onCompletion(mMediaPlayer);
                stayAwake(false);
                return;

            case MEDIA_STOPPED:
                if (mTimeProvider != null) {
                    mTimeProvider.onStopped();
                }
                break;

            //case MEDIA_STARTED:
            case MEDIA_PAUSED:
                if (mTimeProvider != null) {
                    mTimeProvider.onPaused(msg.what == MEDIA_PAUSED);
                }
                if(SystemProperties.getBoolean("media.player.report.udp", false)){
                    int starttime = getCurrentPosition();
                    long time = System.currentTimeMillis()/1000;
                    if(null == info)
                        info= getMediaInfo();
                    StringBuffer buf=new StringBuffer();
                    buf.append("<type>"+"Pause"+"</type>");
                    buf.append('\n');
                    buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                    buf.append('\n');
                    buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                    buf.append('\n');		
                    buf.append("<pauseseconds>"+String.valueOf((float)starttime/1000)+"</pauseseconds>");
                    sendDataReport(buf.toString(), buf.length());
                    Log.e(TAG, buf.length()+" ,:"+buf.toString());
                }
                else if (mContext != null){
			    mPauseFlag = true;
                    int starttime = getCurrentPosition();
                    long end = System.currentTimeMillis();
                    cmd.putExtra("TYPE", "PAUSE_MESSAGE");
                    cmd.putExtra("PLAY_TIME", starttime/1000);
                    cmd.putExtra("TIME", System.currentTimeMillis());
                    if(null == info)
                        info= getMediaInfo();
                    cmd.putExtra("ID", playerId);
                    if(!mbIsFileDescriptor){
                        mContext.sendBroadcast(cmd);
                    }
                }
				// huawei plugin start {{
				if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
					int position = getCurrentPosition()/1000;
					onEvent(HuaweiUtils.ACTION_PAUSE, HuaweiUtils.PARAM_POSITION + "=" + position);
//				+ "&" + HuaweiUtils.PARAM_VIDEO_SIZE + "=" + videoSize); 
				}
                break;

            case MEDIA_BUFFERING_UPDATE:
                if (mOnBufferingUpdateListener != null)
                    mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer, msg.arg1);
                return;

            case MEDIA_SEEK_COMPLETE:
              if (mOnSeekCompleteListener != null) {
                  mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
                  if (subtitleTotal() > 0) {
                    subtitleResetForSeek();
                  }
              }
              // fall through
              if (mContext != null && mSeekStartSend == true){
		  mSeekEndTime = System.currentTimeMillis();
                  long end = System.currentTimeMillis();
                  cmd.putExtra("PLAY_TIME", mSeekTime/1000);
                  cmd.putExtra("TYPE", "SEEK_END");
                  cmd.putExtra("END_TIME", end);
                  if(null == info)
                      info= getMediaInfo();
                  cmd.putExtra("ID", playerId);
                  if(!mbIsFileDescriptor){
                      mContext.sendBroadcast(cmd);
		              mSeekStartSend = false;
                  }
		  mEventHandler.postDelayed(seekBufferEvent,8000);
              }
			  // huawei plugin start {{
			  if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
				  int position = getCurrentPosition()/1000;
				  onEvent(HuaweiUtils.ACTION_SEEKEND, HuaweiUtils.PARAM_POSITION + "=" + position);
//				+ "&" + HuaweiUtils.PARAM_VIDEO_SIZE + "=" + videoSize); 
    }
	      return;

            case MEDIA_SKIPPED:
              if (mTimeProvider != null) {
                  mTimeProvider.onSeekComplete(mMediaPlayer);
              }
              return;

            case MEDIA_SET_VIDEO_SIZE:
              if (mOnVideoSizeChangedListener != null)
                  mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer, msg.arg1, msg.arg2);
              return;

            case MEDIA_ERROR:
              Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
              boolean error_was_handled = false;
              if(SystemProperties.getBoolean("media.player.gd_report.enable", false)) {
                  if(!(msg.arg1 == 301)) {
                      msg.arg1 = 54009;
                  }else{
                      msg.arg1 = msg.arg2;
                  }
                  Log.e(TAG, "gddy report error code (" + msg.arg1 + "," + msg.arg2 + ")");
              }
			  //add by zhaolianghua for drm @20181115
	      if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))&&msg.arg1==302
			      &&msg.arg2<0){
		      Log.d(TAG,"error code for DRM");
		      msg.arg1 = 10000;
		      /*100002 renzheng ; 100001 shouquan*/
		      msg.arg2 = (msg.arg2==-1)?100002:(msg.arg2==-2?100001:msg.arg2);
		      Log.d(TAG,"error for DRM :: what = "+msg.arg1+" ; extra = "+msg.arg2);
	      }
		  //add by zhaolianghua end
              Log.w(TAG, "media event: ERROR BROADCAST\n");
              if (mOnErrorListener != null) {
                  error_was_handled = mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
              }
              if (mOnCompletionListener != null && ! error_was_handled) {
                  mOnCompletionListener.onCompletion(mMediaPlayer);
              }
              stayAwake(false);
              return;

            case MEDIA_INFO:
                switch (msg.arg1) {
                case MEDIA_INFO_VIDEO_TRACK_LAGGING:
                case MEDIA_INFO_NETWORK_BANDWIDTH:
                case MEDIA_INFO_PLAYING_BITRATE:
                    Log.i(TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                    break;
                case MEDIA_INFO_METADATA_UPDATE:
                    scanInternalSubtitleTracks();
                    // fall through

                case MEDIA_INFO_EXTERNAL_METADATA_UPDATE:
                    msg.arg1 = MEDIA_INFO_METADATA_UPDATE;
                    // update default track selection
                    mSubtitleController.selectDefaultTrack();
                    break;
                case MEDIA_INFO_BUFFERING_BROADCAST_START:
                    if(SystemProperties.getBoolean("media.player.report.udp", false)){
                        mBufferStartTime = System.currentTimeMillis();
                        long time = mBufferStartTime /1000;
                        if(null == info)
                            info= getMediaInfo();
                        StringBuffer buf=new StringBuffer();
                        buf.append("<type>"+"BufferStart"+"</type>");
                        buf.append('\n');
                        buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                        buf.append('\n');
                        buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                        sendDataReport(buf.toString(), buf.length());
                        Log.e(TAG, buf.length()+" ,:"+buf.toString());
                    } else if(mContext != null){
                        long start3 = System.currentTimeMillis();
                        Log.i(TAG, "currentTimeMillis =  " + start3);
                        int starttime = getCurrentPosition();
                        cmd.putExtra("TYPE", "BUFFER_START");
                        cmd.putExtra("START_TIME", start3);
                        cmd.putExtra("PLAY_TIME", starttime/1000);
                        if(null == info)
                            info= getMediaInfo();
                        cmd.putExtra("ID", playerId);
			//begin by ysten.zhangjunjian for buffer symbolic
			SystemProperties.set("sys.yst.mbufferstatus", "0");
			//end by ysten.zhangjunjian for buffer symbolic
			            mBufferStartFlag = true;
						//add by ysten-mark for heilongjiang sqm bufferstart
						if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
							int position = getCurrentPosition()/1000;
							onEvent(HuaweiUtils.ACTION_BUFFERSTART, HuaweiUtils.PARAM_POSITION + "=" + position);
						}
                        //begin:add by zhanghk at 20190525:fix report BUFFER after seek problem
                        if(!mbIsFileDescriptor){
			    if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang") || SystemProperties.get("ro.ysten.province", "master").contains("heilongjiang")){
				if(!mSeekFlag){
				    mContext.sendBroadcast(cmd); 
				    mBufferingStartSend = true;
				}
			    }else if(mBufferingStartSend && SystemProperties.get("ro.ysten.province","master").contains("liaoning")){
                                mBufferingStartSend = false;
                                cmdBuffer = cmd;
                                cmdBuffer.putExtra("TYPE", "BUFFER_END");
                                cmdBuffer.putExtra("END_TIME", System.currentTimeMillis());
                                mContext.sendBroadcast(cmdBuffer);
				break;
			    }else {
				mContext.sendBroadcast(cmd);
				mBufferingStartSend = true;
			    }
                        }
                        //end:add by zhanghk at 20190525:fix report BUFFER after seek problem
                    }
                    return;

                case MEDIA_INFO_BUFFERING_BROADCAST_END:
                    if(SystemProperties.getBoolean("media.player.report.udp", false)){
						mBufferEndTime = System.currentTimeMillis();
                        long time = System.currentTimeMillis()/1000;
                        if(null == info)
                            info= getMediaInfo();
                        StringBuffer buf=new StringBuffer();
                        buf.append("<type>"+"BufferStart"+"</type>");
                        buf.append('\n');
                        buf.append("<contentid>"+String.valueOf(playerId)+"</contentid>");
                        buf.append('\n');
                        buf.append("<utcsecond>"+String.valueOf(time)+"</utcsecond>");
                        buf.append('\n');		
                        buf.append("<bufferseconds>"+String.valueOf(time-mBufferStartTime)+"</bufferseconds>");
                        sendDataReport(buf.toString(), buf.length());
                        Log.e(TAG,buf.length()+" ,:"+buf.toString());
                    } else if(mContext != null && mBufferingStartSend == true){
			   mBufferStartFlag = false;
                        long end = System.currentTimeMillis();
			//begin:add by liangk at 200312:send buffering end in 5s after seek end for liaoning
			if(SystemProperties.get("ro.ysten.province").contains("liaoning")){
                        	if((end < mSeekEndTime+5000)){
                            		Log.i(TAG, "don't send buffering end in 5s after seek end");
                            		break;
                        	}
			}
			//end:add by liangk at 200312:send buffering end in 5s after seek end for liaoning
                        if((end < mSeekEndTime+5000)&&SystemProperties.getBoolean("media.player.delaybuf.seek", false)){
                            Log.i(TAG, "don't send buffering end in 5s after seek end");
                            break;
                        }
                        cmd.putExtra("TYPE", "BUFFER_END");
                        cmd.putExtra("END_TIME", end);
                        if(null == info)
                            info= getMediaInfo();
                        cmd.putExtra("ID", playerId);
                        //begin:add by zhanghk at 20190525:fix report BUFFER after seek problem
                        //begin:add by mark:fix report BUFFER after seek problem
					if(SystemProperties.get("ro.ysten.province").contains("heilongjiang")){
                    int position = getCurrentPosition()/1000;
                    onEvent(HuaweiUtils.ACTION_BUFFEREND, HuaweiUtils.PARAM_POSITION + "=" + position);
//				+ "&" + HuaweiUtils.PARAM_VIDEO_SIZE + "=" + videoSize); 
                    }
                        if((!mbIsFileDescriptor)&& SystemProperties.get("sys.yst.mbufferstatus", "1").equals("0")){
                            SystemProperties.set("sys.yst.mbufferstatus", "1");
			    if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
				|| SystemProperties.get("ro.ysten.province", "master").contains("heilongjiang")){
				if(!mSeekFlag){
                                    mContext.sendBroadcast(cmd);
			            mBufferingStartSend = false;
			        }
			    }else{
				mContext.sendBroadcast(cmd);
				mBufferingStartSend = false;
			    }
                        }
                        //end:add by zhanghk at 20190525:fix report BUFFER after seek problem
                    }
                    return;
					
                case MEDIA_INFO_AMLOGIC_NO_AUDIO:
                case MEDIA_INFO_AMLOGIC_NO_VIDEO:
                case MEDIA_INFO_AMLOGIC_AUDIO_NOT_SUPPORT:
                case MEDIA_INFO_AMLOGIC_VIDEO_NOT_SUPPORT:
                case MEDIA_ERROR_PLAYER_REPORT:
                    if(SystemProperties.getBoolean("media.player.cmcc_report.enable", false)) {
                        if(mContext != null) {
                            if (msg.obj instanceof Parcel) {
                                Parcel parcel = (Parcel) msg.obj;
                                player_id = parcel.readInt();
                                parcel.recycle();
                                Log.e(TAG,"player_id: "+ player_id);
                            }

                            cmd.putExtra("ID", playerId);
                            cmd.putExtra("TYPE", "ERROR_MESSAGE");
                            Log.w(TAG, "ERROR_CODE "+ msg.arg2);
                            cmd.putExtra("ERROR_CODE", msg.arg2);
                            cmd.putExtra("TIME", System.currentTimeMillis());
                            if(!mbIsFileDescriptor){
                                mContext.sendBroadcast(cmd);
                            }
                        }
                    }
                    break;
                }
		case MEDIA_INFO_BUFFERING_START:
		case MEDIA_INFO_BUFFERING_END:

                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(mMediaPlayer, msg.arg1, msg.arg2);
                }
                // No real default action so far.
                return;
            case MEDIA_TIMED_TEXT:
                if (mOnTimedTextListener == null)
                    return;
                if (msg.obj == null) {
                    mOnTimedTextListener.onTimedText(mMediaPlayer, null);
                } else {
                    if (msg.obj instanceof Parcel) {
                        Parcel parcel = (Parcel)msg.obj;
                        TimedText text = new TimedText(parcel);
                        parcel.recycle();
                        mOnTimedTextListener.onTimedText(mMediaPlayer, text);
                    }
                }
                return;

            case MEDIA_SUBTITLE_DATA:
                if (mOnSubtitleDataListener == null) {
                    return;
                }
                if (msg.obj == null) {
                    Log.e(TAG,"MEDIA_SUBTITLE_DATA,msg.obj=null-");
                    mOnSubtitleDataListener.onSubtitleData(mMediaPlayer, null);
                }else{
                if (msg.obj instanceof Parcel) {
                    Parcel parcel = (Parcel) msg.obj;
                    Bitmap bitmap = getSubtitleBitmap();
                    SubtitleData data = new SubtitleData(parcel, bitmap);
                    parcel.recycle();
                    mOnSubtitleDataListener.onSubtitleData(mMediaPlayer, data);
                    }
                }
                return;

            case MEDIA_AML_SUBTITLE_START:
                if(subtitleServiceDebug()) Log.i(TAG,"[handleMessage]MEDIA_AML_SUBTITLE_START mPath:"+mPath);
                if(mPath != null) {
                    if(!mSubtitleLoad) {
		        int ret = subtitleOpen(mPath);
		        if(ret == 0) {
		            subtitleShow();
		            if(subtitleOptionEnable()) {
		                subtitleOption();//show subtitle select option add for debug
		            }
		        }
                    }
                    else {
                    	subtitleShow();
                    }
                }
                break;

            case MEDIA_BLURAY_INFO:
                if (mOnBlurayInfoListener == null)
                    return;
                mOnBlurayInfoListener.onBlurayInfo(mMediaPlayer, msg.arg1, msg.arg2, msg.obj);
                return;

            case MEDIA_NOP: // interface test message - ignore
                break;

            case MEDIA_INFO_REPORT:
                updateMediaReportParam();
                break;

            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }catch(Exception e){
			Log.e(TAG, "huxiang add error catch"+e.getMessage());
            return;
		}
		}
    }


    private boolean isEjectOrUnmoutProcessed = false;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Uri uri = intent.getData();
            String path = uri.getPath();

            //Log.d("wxl", "mountreciever action=" + action + " uri=" + uri + " path=" + path);
            if (action == null ||path == null)
                return;

            if ((action.equals(Intent.ACTION_MEDIA_EJECT))||(action.equals(Intent.ACTION_MEDIA_UNMOUNTED))) {
                if(mPath != null) {
                    if(mPath.startsWith(path)) {
                        if(isEjectOrUnmoutProcessed)
                            return;
                        else
                            isEjectOrUnmoutProcessed = true;

                        stop();

                        //add for gallery finish
                        if (mEventHandler != null) {
                            Message m = mEventHandler.obtainMessage(MEDIA_ERROR);
                            mEventHandler.sendMessage(m);
                        }
                    }
                }
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                // Nothing
            }
        }
    };

    /*
     * Called from native code when an interesting event happens.  This method
     * just uses the EventHandler system to post the event back to the main app thread.
     * We use a weak reference to the original MediaPlayer object so that the native
     * code is safe from the object disappearing from underneath it.  (This is
     * the cookie passed to native_setup().)
     */
    private static void postEventFromNative(Object mediaplayer_ref,
                                            int what, int arg1, int arg2, Object obj)
    {
       Log.v(TAG, "what... " + what+ " arg1... " + arg1+"arg2...."+arg2+"obj..."+obj);

        MediaPlayer mp = (MediaPlayer)((WeakReference)mediaplayer_ref).get();
        if (mp == null) {
         Log.v(TAG, "mp is null .... "+what );
            return;
        }

        if (what == MEDIA_INFO && arg1 == MEDIA_INFO_STARTED_AS_NEXT) {
            // this acquires the wakelock if needed, and sets the client side state
           Log.v(TAG, "111 "+what );
            mp.start();
        }
        if (mp.mEventHandler != null) {
            Log.v(TAG, "222 "+what );
            Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mp.mEventHandler.sendMessage(m);
        }
    }

    /**
     * Interface definition for a callback to be invoked when the media
     * source is ready for playback.
     */
    public interface OnPreparedListener
    {
        /**
         * Called when the media file is ready for playback.
         *
         * @param mp the MediaPlayer that is ready for playback
         */
        void onPrepared(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when the media source is ready
     * for playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnPreparedListener(OnPreparedListener listener)
    {
        mOnPreparedListener = listener;
    }

    private OnPreparedListener mOnPreparedListener;

    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCompletionListener
    {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when the end of a media source
     * has been reached during playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener listener)
    {
        mOnCompletionListener = listener;
    }

    private OnCompletionListener mOnCompletionListener;

    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a media resource being streamed over the network.
     */
    public interface OnBufferingUpdateListener
    {
        /**
         * Called to update status in buffering a media stream received through
         * progressive HTTP download. The received buffering percentage
         * indicates how much of the content has been buffered or played.
         * For example a buffering update of 80 percent when half the content
         * has already been played indicates that the next 30 percent of the
         * content to play has been buffered.
         *
         * @param mp      the MediaPlayer the update pertains to
         * @param percent the percentage (0-100) of the content
         *                that has been buffered or played thus far
         */
        void onBufferingUpdate(MediaPlayer mp, int percent);
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener)
    {
        mOnBufferingUpdateListener = listener;
    }

    private OnBufferingUpdateListener mOnBufferingUpdateListener;

    /**
     * Interface definition of a callback to be invoked indicating
     * the completion of a seek operation.
     */
    public interface OnSeekCompleteListener
    {
        /**
         * Called to indicate the completion of a seek operation.
         *
         * @param mp the MediaPlayer that issued the seek operation
         */
        public void onSeekComplete(MediaPlayer mp);
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener)
    {
        mOnSeekCompleteListener = listener;
    }

    private OnSeekCompleteListener mOnSeekCompleteListener;

    /**
     * Interface definition of a callback to be invoked when the
     * video size is first known or updated
     */
    public interface OnVideoSizeChangedListener
    {
        /**
         * Called to indicate the video size
         *
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         *
         * @param mp        the MediaPlayer associated with this callback
         * @param width     the width of the video
         * @param height    the height of the video
         */
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height);
    }

    /**
     * Register a callback to be invoked when the video size is
     * known or updated.
     *
     * @param listener the callback that will be run
     */
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener)
    {
        mOnVideoSizeChangedListener = listener;
    }

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;

    /**
     * Interface definition of a callback to be invoked when a
     * timed text is available for display.
     */
    public interface OnTimedTextListener
    {
        /**
         * Called to indicate an avaliable timed text
         *
         * @param mp             the MediaPlayer associated with this callback
         * @param text           the timed text sample which contains the text
         *                       needed to be displayed and the display format.
         */
        public void onTimedText(MediaPlayer mp, TimedText text);
    }

    /**
     * Register a callback to be invoked when a timed text is available
     * for display.
     *
     * @param listener the callback that will be run
     */
    public void setOnTimedTextListener(OnTimedTextListener listener)
    {
        mOnTimedTextListener = listener;
    }

    private OnTimedTextListener mOnTimedTextListener;

    /**
     * Interface definition of a callback to be invoked when a
     * track has data available.
     *
     * @hide
     */
    public interface OnSubtitleDataListener
    {
        public void onSubtitleData(MediaPlayer mp, SubtitleData data);
    }

    /**
     * Register a callback to be invoked when a track has data available.
     *
     * @param listener the callback that will be run
     *
     * @hide
     */
    public void setOnSubtitleDataListener(OnSubtitleDataListener listener)
    {
        mOnSubtitleDataListener = listener;
    }

    private OnSubtitleDataListener mOnSubtitleDataListener;

    /**
     * Interface definition of a callback to be invoked when a
     * bluray infomation is available for update.
     */
    public interface OnBlurayListener
    {
        /**
         * Called to indicate an avaliable bluray info
         *
         * @param mp             the MediaPlayer associated with this callback
         * @param ext1           the bluray info message arg1
         * @param ext2           the bluray info message arg2
         * @param obj            the bluray info message obj
         */
        public void onBlurayInfo(MediaPlayer mp, int ext1, int ext2, Object obj);
    }

    /**
     * Register a callback to be invoked when a bluray info is available
     * for update.
     *
     * @param listener the callback that will be run
     */
    public void setOnBlurayInfoListener(OnBlurayListener listener)
    {
        mOnBlurayInfoListener = listener;
    }

    private OnBlurayListener mOnBlurayInfoListener;

    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    /** Unspecified media player error.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;

    /** Media server died. In this case, the application must release the
     * MediaPlayer object and instantiate a new one.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100;

    /** The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;

    /** File or network related operation errors. */
    public static final int MEDIA_ERROR_IO = -1004;
    /** Bitstream is not conforming to the related coding standard or file spec. */
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    /** Bitstream is conforming to the related coding standard or file spec, but
     * the media framework does not support the feature. */
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    /** Some operation takes too long to complete, usually more than 3-5 seconds. */
    public static final int MEDIA_ERROR_TIMED_OUT = -110;

    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during an asynchronous operation (other errors
     * will throw exceptions at method call time).
     */
    public interface OnErrorListener
    {
        /**
         * Called to indicate an error.
         *
         * @param mp      the MediaPlayer the error pertains to
         * @param what    the type of error that has occurred:
         * <ul>
         * <li>{@link #MEDIA_ERROR_UNKNOWN}
         * <li>{@link #MEDIA_ERROR_SERVER_DIED}
         * </ul>
         * @param extra an extra code, specific to the error. Typically
         * implementation dependent.
         * <ul>
         * <li>{@link #MEDIA_ERROR_IO}
         * <li>{@link #MEDIA_ERROR_MALFORMED}
         * <li>{@link #MEDIA_ERROR_UNSUPPORTED}
         * <li>{@link #MEDIA_ERROR_TIMED_OUT}
         * </ul>
         * @return True if the method handled the error, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the OnCompletionListener to be called.
         */
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error has happened
     * during an asynchronous operation.
     *
     * @param listener the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener listener)
    {
        mOnErrorListener = listener;
    }

    private OnErrorListener mOnErrorListener;


    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    /** Unspecified media player info.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_UNKNOWN = 1;

    /** The player was started because it was used as the next player for another
     * player, which just completed playback.
     * @see android.media.MediaPlayer.OnInfoListener
     * @hide
     */
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;

    /** The player just pushed the very first video frame for rendering.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;

    /** The video is too complex for the decoder: it can't decode frames fast
     *  enough. Possibly only the audio plays fine at this stage.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

    /** MediaPlayer is temporarily pausing playback internally in order to
     * buffer more data.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /** MediaPlayer is resuming playback after filling buffers.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    public static final int MEDIA_INFO_PLAYING_BITRATE = 705;

    public static final int MEDIA_INFO_BUFFERING_BROADCAST_START = 706;
    public static final int MEDIA_INFO_BUFFERING_BROADCAST_END = 707;

    /** Bad interleaving means that a media has been improperly interleaved or
     * not interleaved at all, e.g has all the video samples first then all the
     * audio ones. Video is playing but a lot of disk seeks may be happening.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

    /** The media cannot be seeked (e.g live stream)
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

    /** A new set of metadata is available.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;

    /** A new set of external-only metadata is available.  Used by
     *  JAVA framework to avoid triggering track scanning.
     * @hide
     */
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;

    /** Failed to handle timed text track properly.
     * @see android.media.MediaPlayer.OnInfoListener
     *
     * {@hide}
     */
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
	//added for save log
    public static final int MEDIA_INFO_DOWNLOAD_START = 10086;
    public static final int MEDIA_INFO_DOWNLOAD_END = 10087;
    public static final int MEDIA_INFO_DOWNLOAD_ERROR = 10088;

	public static final int MEDIA_INFO_HLS_SEGMENT = 10085;
	public static final int MEDIA_INFO_HTTP_CONNECT_OK = 10089;
	public static final int MEDIA_INFO_HTTP_CODE = 10090;
	public static final int MEDIA_INFO_HTTP_LOCATION = 10091;
	public static final int  MEDIA_INFO_HTTP_REDIRECT = 10092;
	public static final int  MEDIA_INFO_LIVE_SHIFT = 10093;
	public static final int MEDIA_INFO_HTTP_CONNECT_ERROR = 10094;

    /** Subtitle track was not supported by the media framework.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;

    /** Reading the subtitle track takes too long.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the media or its playback.
     */
    public interface OnInfoListener
    {
        /**
         * Called to indicate an info or a warning.
         *
         * @param mp      the MediaPlayer the info pertains to.
         * @param what    the type of info or warning.
         * <ul>
         * <li>{@link #MEDIA_INFO_UNKNOWN}
         * <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
         * <li>{@link #MEDIA_INFO_VIDEO_RENDERING_START}
         * <li>{@link #MEDIA_INFO_BUFFERING_START}
         * <li>{@link #MEDIA_INFO_BUFFERING_END}
         * <li>{@link #MEDIA_INFO_BAD_INTERLEAVING}
         * <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
         * <li>{@link #MEDIA_INFO_METADATA_UPDATE}
         * <li>{@link #MEDIA_INFO_UNSUPPORTED_SUBTITLE}
         * <li>{@link #MEDIA_INFO_SUBTITLE_TIMED_OUT}
         * </ul>
         * @param extra an extra code, specific to the info. Typically
         * implementation dependent.
         * @return True if the method handled the info, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the info to be discarded.
         */
        boolean onInfo(MediaPlayer mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an info/warning is available.
     *
     * @param listener the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener)
    {
        mOnInfoListener = listener;
    }

    private OnInfoListener mOnInfoListener;

    /*
     * Test whether a given video scaling mode is supported.
     */
    private boolean isVideoScalingModeSupported(int mode) {
        return (mode == VIDEO_SCALING_MODE_SCALE_TO_FIT ||
                mode == VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
    }

    private Context mProxyContext = null;
    private ProxyReceiver mProxyReceiver = null;

    private void setupProxyListener(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Proxy.PROXY_CHANGE_ACTION);
        mProxyReceiver = new ProxyReceiver();
        mProxyContext = context;

        Intent currentProxy =
            context.getApplicationContext().registerReceiver(mProxyReceiver, filter);

        if (currentProxy != null) {
            handleProxyBroadcast(currentProxy);
        }
    }

    private void disableProxyListener() {
        if (mProxyReceiver == null) {
            return;
        }

        Context appContext = mProxyContext.getApplicationContext();
        if (appContext != null) {
            appContext.unregisterReceiver(mProxyReceiver);
        }

        mProxyReceiver = null;
        mProxyContext = null;
    }

    private void handleProxyBroadcast(Intent intent) {
        ProxyProperties props =
            (ProxyProperties)intent.getExtra(Proxy.EXTRA_PROXY_INFO);

        if (props == null || props.getHost() == null) {
            updateProxyConfig(null);
        } else {
            updateProxyConfig(props);
        }
    }

    private class ProxyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Proxy.PROXY_CHANGE_ACTION)) {
                handleProxyBroadcast(intent);
            }
        }
    }

    private native void updateProxyConfig(ProxyProperties props);

    /** @hide */
    static class TimeProvider implements MediaPlayer.OnSeekCompleteListener,
            MediaTimeProvider {
        private static final String TAG = "MTP";
        private static final long MAX_NS_WITHOUT_POSITION_CHECK = 5000000000L;
        private static final long MAX_EARLY_CALLBACK_US = 1000;
        private static final long TIME_ADJUSTMENT_RATE = 2;  /* meaning 1/2 */
        private long mLastTimeUs = 0;
        private MediaPlayer mPlayer;
        private boolean mPaused = true;
        private boolean mStopped = true;
        private long mLastReportedTime;
        private long mTimeAdjustment;
        // since we are expecting only a handful listeners per stream, there is
        // no need for log(N) search performance
        private MediaTimeProvider.OnMediaTimeListener mListeners[];
        private long mTimes[];
        private long mLastNanoTime;
        private Handler mEventHandler;
        private boolean mRefresh = false;
        private boolean mPausing = false;
        private boolean mSeeking = false;
        private static final int NOTIFY = 1;
        private static final int NOTIFY_TIME = 0;
        private static final int REFRESH_AND_NOTIFY_TIME = 1;
        private static final int NOTIFY_STOP = 2;
        private static final int NOTIFY_SEEK = 3;
        private HandlerThread mHandlerThread;

        /** @hide */
        public boolean DEBUG = false;

        public TimeProvider(MediaPlayer mp) {
            mPlayer = mp;
            try {
                getCurrentTimeUs(true, false);
            } catch (IllegalStateException e) {
                // we assume starting position
                mRefresh = true;
            }

            Looper looper;
            if ((looper = Looper.myLooper()) == null &&
                (looper = Looper.getMainLooper()) == null) {
                // Create our own looper here in case MP was created without one
                mHandlerThread = new HandlerThread("MediaPlayerMTPEventThread",
                      Process.THREAD_PRIORITY_FOREGROUND);
                mHandlerThread.start();
                looper = mHandlerThread.getLooper();
            }
            mEventHandler = new EventHandler(looper);

            mListeners = new MediaTimeProvider.OnMediaTimeListener[0];
            mTimes = new long[0];
            mLastTimeUs = 0;
            mTimeAdjustment = 0;
        }

        private void scheduleNotification(int type, long delayUs) {
            // ignore time notifications until seek is handled
            if (mSeeking &&
                    (type == NOTIFY_TIME || type == REFRESH_AND_NOTIFY_TIME)) {
                return;
            }

            if (DEBUG) Log.v(TAG, "scheduleNotification " + type + " in " + delayUs);
            mEventHandler.removeMessages(NOTIFY);
            Message msg = mEventHandler.obtainMessage(NOTIFY, type, 0);
            mEventHandler.sendMessageDelayed(msg, (int) (delayUs / 1000));
        }

        /** @hide */
        public void close() {
            mEventHandler.removeMessages(NOTIFY);
            if (mHandlerThread != null) {
                mHandlerThread.quitSafely();
                mHandlerThread = null;
            }
        }

        /** @hide */
        protected void finalize() {
            if (mHandlerThread != null) {
                mHandlerThread.quitSafely();
            }
        }

        /** @hide */
        public void onPaused(boolean paused) {
            synchronized(this) {
                if (DEBUG) Log.d(TAG, "onPaused: " + paused);
                if (mStopped) { // handle as seek if we were stopped
                    mStopped = false;
                    mSeeking = true;
                    scheduleNotification(NOTIFY_SEEK, 0 /* delay */);
                } else {
                    mPausing = paused;  // special handling if player disappeared
                    mSeeking = false;
                    scheduleNotification(REFRESH_AND_NOTIFY_TIME, 0 /* delay */);
                }
            }
        }

        /** @hide */
        public void onStopped() {
            synchronized(this) {
                if (DEBUG) Log.d(TAG, "onStopped");
                mPaused = true;
                mStopped = true;
                mSeeking = false;
                scheduleNotification(NOTIFY_STOP, 0 /* delay */);
            }
        }

        /** @hide */
        @Override
        public void onSeekComplete(MediaPlayer mp) {
            synchronized(this) {
                mStopped = false;
                mSeeking = true;
                scheduleNotification(NOTIFY_SEEK, 0 /* delay */);
            }
        }

        /** @hide */
        public void onNewPlayer() {
            if (mRefresh) {
                synchronized(this) {
                    mStopped = false;
                    mSeeking = true;
                    scheduleNotification(NOTIFY_SEEK, 0 /* delay */);
                }
            }
        }

        private synchronized void notifySeek() {
            mSeeking = false;
            try {
                long timeUs = getCurrentTimeUs(true, false);
                if (DEBUG) Log.d(TAG, "onSeekComplete at " + timeUs);

                for (MediaTimeProvider.OnMediaTimeListener listener: mListeners) {
                    if (listener == null) {
                        break;
                    }
                    listener.onSeek(timeUs);
                }
            } catch (IllegalStateException e) {
                // we should not be there, but at least signal pause
                if (DEBUG) Log.d(TAG, "onSeekComplete but no player");
                mPausing = true;  // special handling if player disappeared
                notifyTimedEvent(false /* refreshTime */);
            }
        }

        private synchronized void notifyStop() {
            for (MediaTimeProvider.OnMediaTimeListener listener: mListeners) {
                if (listener == null) {
                    break;
                }
                listener.onStop();
            }
        }

        private int registerListener(MediaTimeProvider.OnMediaTimeListener listener) {
            int i = 0;
            for (; i < mListeners.length; i++) {
                if (mListeners[i] == listener || mListeners[i] == null) {
                    break;
                }
            }

            // new listener
            if (i >= mListeners.length) {
                MediaTimeProvider.OnMediaTimeListener[] newListeners =
                    new MediaTimeProvider.OnMediaTimeListener[i + 1];
                long[] newTimes = new long[i + 1];
                System.arraycopy(mListeners, 0, newListeners, 0, mListeners.length);
                System.arraycopy(mTimes, 0, newTimes, 0, mTimes.length);
                mListeners = newListeners;
                mTimes = newTimes;
            }

            if (mListeners[i] == null) {
                mListeners[i] = listener;
                mTimes[i] = MediaTimeProvider.NO_TIME;
            }
            return i;
        }

        public void notifyAt(
                long timeUs, MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized(this) {
                if (DEBUG) Log.d(TAG, "notifyAt " + timeUs);
                mTimes[registerListener(listener)] = timeUs;
                scheduleNotification(NOTIFY_TIME, 0 /* delay */);
            }
        }

        public void scheduleUpdate(MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized(this) {
                if (DEBUG) Log.d(TAG, "scheduleUpdate");
                int i = registerListener(listener);

                if (mStopped) {
                    scheduleNotification(NOTIFY_STOP, 0 /* delay */);
                } else {
                    mTimes[i] = 0;
                    scheduleNotification(NOTIFY_TIME, 0 /* delay */);
                }
            }
        }

        public void cancelNotifications(
                MediaTimeProvider.OnMediaTimeListener listener) {
            synchronized(this) {
                int i = 0;
                for (; i < mListeners.length; i++) {
                    if (mListeners[i] == listener) {
                        System.arraycopy(mListeners, i + 1,
                                mListeners, i, mListeners.length - i - 1);
                        System.arraycopy(mTimes, i + 1,
                                mTimes, i, mTimes.length - i - 1);
                        mListeners[mListeners.length - 1] = null;
                        mTimes[mTimes.length - 1] = NO_TIME;
                        break;
                    } else if (mListeners[i] == null) {
                        break;
                    }
                }

                scheduleNotification(NOTIFY_TIME, 0 /* delay */);
            }
        }

        private synchronized void notifyTimedEvent(boolean refreshTime) {
            // figure out next callback
            long nowUs;
            try {
                nowUs = getCurrentTimeUs(refreshTime, true);
            } catch (IllegalStateException e) {
                // assume we paused until new player arrives
                mRefresh = true;
                mPausing = true; // this ensures that call succeeds
                nowUs = getCurrentTimeUs(refreshTime, true);
            }
            long nextTimeUs = nowUs;

            if (mSeeking) {
                // skip timed-event notifications until seek is complete
                return;
            }

            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("notifyTimedEvent(").append(mLastTimeUs).append(" -> ")
                        .append(nowUs).append(") from {");
                boolean first = true;
                for (long time: mTimes) {
                    if (time == NO_TIME) {
                        continue;
                    }
                    if (!first) sb.append(", ");
                    sb.append(time);
                    first = false;
                }
                sb.append("}");
                Log.d(TAG, sb.toString());
            }

            Vector<MediaTimeProvider.OnMediaTimeListener> activatedListeners =
                new Vector<MediaTimeProvider.OnMediaTimeListener>();
            for (int ix = 0; ix < mTimes.length; ix++) {
                if (mListeners[ix] == null) {
                    break;
                }
                if (mTimes[ix] <= NO_TIME) {
                    // ignore, unless we were stopped
                } else if (mTimes[ix] <= nowUs + MAX_EARLY_CALLBACK_US) {
                    activatedListeners.add(mListeners[ix]);
                    if (DEBUG) Log.d(TAG, "removed");
                    mTimes[ix] = NO_TIME;
                } else if (nextTimeUs == nowUs || mTimes[ix] < nextTimeUs) {
                    nextTimeUs = mTimes[ix];
                }
            }

            if (nextTimeUs > nowUs && !mPaused) {
                // schedule callback at nextTimeUs
                if (DEBUG) Log.d(TAG, "scheduling for " + nextTimeUs + " and " + nowUs);
                scheduleNotification(NOTIFY_TIME, nextTimeUs - nowUs);
            } else {
                mEventHandler.removeMessages(NOTIFY);
                // no more callbacks
            }

            for (MediaTimeProvider.OnMediaTimeListener listener: activatedListeners) {
                listener.onTimedEvent(nowUs);
            }
        }

        private long getEstimatedTime(long nanoTime, boolean monotonic) {
            if (mPaused) {
                mLastReportedTime = mLastTimeUs + mTimeAdjustment;
            } else {
                long timeSinceRead = (nanoTime - mLastNanoTime) / 1000;
                mLastReportedTime = mLastTimeUs + timeSinceRead;
                if (mTimeAdjustment > 0) {
                    long adjustment =
                        mTimeAdjustment - timeSinceRead / TIME_ADJUSTMENT_RATE;
                    if (adjustment <= 0) {
                        mTimeAdjustment = 0;
                    } else {
                        mLastReportedTime += adjustment;
                    }
                }
            }
            return mLastReportedTime;
        }

        public long getCurrentTimeUs(boolean refreshTime, boolean monotonic)
                throws IllegalStateException {
            synchronized (this) {
                // we always refresh the time when the paused-state changes, because
                // we expect to have received the pause-change event delayed.
                if (mPaused && !refreshTime) {
                    return mLastReportedTime;
                }

                long nanoTime = System.nanoTime();
                if (refreshTime ||
                        nanoTime >= mLastNanoTime + MAX_NS_WITHOUT_POSITION_CHECK) {
                    try {
                        mLastTimeUs = mPlayer.getCurrentPosition() * 1000;
                        mPaused = !mPlayer.isPlaying();
                        if (DEBUG) Log.v(TAG, (mPaused ? "paused" : "playing") + " at " + mLastTimeUs);
                    } catch (IllegalStateException e) {
                        if (mPausing) {
                            // if we were pausing, get last estimated timestamp
                            mPausing = false;
                            getEstimatedTime(nanoTime, monotonic);
                            mPaused = true;
                            if (DEBUG) Log.d(TAG, "illegal state, but pausing: estimating at " + mLastReportedTime);
                            return mLastReportedTime;
                        }
                        // TODO get time when prepared
                        throw e;
                    }
                    mLastNanoTime = nanoTime;
                    if (monotonic && mLastTimeUs < mLastReportedTime) {
                        /* have to adjust time */
                        mTimeAdjustment = mLastReportedTime - mLastTimeUs;
                        if (mTimeAdjustment > 1000000) {
                            // schedule seeked event if time jumped significantly
                            // TODO: do this properly by introducing an exception
                            mStopped = false;
                            mSeeking = true;
                            scheduleNotification(NOTIFY_SEEK, 0 /* delay */);
                        }
                    } else {
                        mTimeAdjustment = 0;
                    }
                }

                return getEstimatedTime(nanoTime, monotonic);
            }
        }

        private class EventHandler extends Handler {
            public EventHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == NOTIFY) {
                    switch (msg.arg1) {
                    case NOTIFY_TIME:
                        notifyTimedEvent(false /* refreshTime */);
                        break;
                    case REFRESH_AND_NOTIFY_TIME:
                        notifyTimedEvent(true /* refreshTime */);
                        break;
                    case NOTIFY_STOP:
                        notifyStop();
                        break;
                    case NOTIFY_SEEK:
                        notifySeek();
                        break;
                    }
                }
            }
        }
    }
    private boolean mbIsFileDescriptor = false;

	@Override
	public CHANNEL switchChannel(CHANNEL channel) {
        boolean switchSuccess = false;
        String audioChannelMode = "lmono";

        switch (channel) {
            case LEFT:
                audioChannelMode = "lmono";
                break;
            case RIGHT:
                audioChannelMode = "rmono";
                break;
            case CENTER:
                audioChannelMode = "stereo";
                break;
            default:
                audioChannelMode = "stereo";
                break;
        }
        switchSuccess = setParameter(KEY_PARAMETER_AML_PLAYER_SWITCH_SOUND_TRACK, audioChannelMode);

        if(switchSuccess) {
            Log.i(TAG, "switchChannel " + audioChannelMode + "success");
            return channel;
        } else {
            Log.e(TAG, "switchChannel " + audioChannelMode + " failed!!");
            return CHANNEL.CENTER;
        }
	}

	@Override
	public boolean canSelectTrackImmediately() {
		return true;
	}

    private native void setprop(String prop, String value);
    private native void sendDataReport(String prop, int value);

    private native String getprop(String prop);

    private native void writeSysfs(String path, String value);

    private native String native_readSysfs(String path);


	/*save player's status in file*/
    private void printLogInfoFromNative(int type ,int ret){
		Log.e(TAG, "printLogInfoFromNative,ret:"+ret);
		String mesg ;
		switch(type){
			case PRINT_LOG_SET_VIDEOSURFACETEXTURE:
				mesg = new String("setVideoSurfaceTexture");
				break;
			case PRINT_LOG_PREPAREASYNC:
				mesg = new String("prepareAysnc start");
				break;
			case PRINT_LOG_SEEKTO:
				mesg = new String("seekTo,totime:");
				mesg += ret;
				mesg += new String("ms");
				break;
			case PRINT_LOG_CURRENT_PLAYTIME:
				mesg = new String("current playtime:");
				mesg += ret;
				mesg += new String("ms");
				break;
			default:
				mesg = new String("WRONG MESG");
				
		}
		if(!mesg.equals("WRONG MESG"))
			save_log_in_file(type,mesg);
	}
    private void save_log_in_file(int type, String mesg){
		if (mEventHandler != null) {
			Log.e(TAG, "save_log_in_file:"+type+" mesg:"+mesg);
			Message m = mEventHandler.obtainMessage(type ,mesg);
			mEventHandler.sendMessage(m);
		}
	}
	

	public class SaveLogThread extends Thread { 
		//private Message mMesg = Message.obtain();
		int msg_what = 0;
		int msg_arg1 = 0;
		int msg_arg2 = 0;
		String msg_obj ;
		public Handler mHandler;
		public SaveLogThread(){
			super();
		}
		
		 @Override	
               public void run() {
                   Looper.prepare();
                   mSavelogLooper = Looper.myLooper();
                   mHandler = new Handler() {
                       public void handleMessage(Message msg) {
                           try{
                               synchronized (mSaveLogLock) {
                                   boolean b_need_recode = false;
                                   switch (msg.what) {
                                       case MEDIA_INFO:
                                       if(msg.arg1 == MEDIA_INFO_HTTP_CONNECT_OK ||
                                           msg.arg1 == MEDIA_INFO_HTTP_CONNECT_ERROR ||
                                           msg.arg1 == MEDIA_INFO_HTTP_CODE ||
                                           msg.arg1 == MEDIA_INFO_HTTP_REDIRECT ||
                                           msg.arg1 == MEDIA_INFO_LIVE_SHIFT ||
                                           msg.arg1 == MEDIA_INFO_BUFFERING_START ||
                                           msg.arg1 == MEDIA_INFO_BUFFERING_END ||
                                           msg.arg1 == MEDIA_PLAYBACK_COMPLETE)
                                           b_need_recode = true;
                                           break;
                                       case MEDIA_SEEK_COMPLETE:
                                       case MEDIA_SET_VIDEO_SIZE:
                                           b_need_recode = true;
                                           break;
                                       case PRINT_LOG_PREPARED:
                                           b_need_recode = true;
                                           break;
                                       case PRINT_LOG_SETPLAYER_SOURCE:
                                       case PRINT_LOG_CREATE_PLAYER:
                                       case PRINT_LOG_SET_VIDEOSURFACETEXTURE:
                                       case PRINT_LOG_PREPAREASYNC:
                                       case PRINT_LOG_CODEC_INFO:
                                       case PRINT_LOG_START:
                                       case PRINT_LOG_PAUSE:
                                       case PRINT_LOG_FIRST_FRMAE_SHOWN:
                                       case PRINT_LOG_SEEKTO:
                                       case PRINT_LOG_STOP:
                                       case PRINT_LOG_START_DECODER:
                                       case PRINT_LOG_PLAYMODE:
                                       case PRINT_LOG_DESTORY_PLAYER:
                                       case PRINT_LOG_TS_DOWNLOAD_BAGIN:
                                       case PRINT_LOG_TS_DOWNLOAD_COMPLETE:
                                       case PRINT_LOG_ERROR:
                                       case PRINT_LOG_CURRENT_PLAYTIME:
                                           b_need_recode = true;
                                           break;
                                   }

                                   if ((b_first_time_symbol) && (b_need_recode)) {
                                       //init base log sys time
                                       if (getFirstFileNameInThisTask()) {
                                           Log.e(TAG, "!!!getFirstFileName,start");
                                           b_first_time_symbol = false;
                                           Timer timer = new Timer();
                                           timer.schedule(new MyTask(),mFirstTime_interval, RECORD_TIME_INTERVAL);
                                       }
                                   }

                                   if (b_need_recode) {
                                       try {
                                           if (DEFAULT_LOGFILE.equals(logfile_name)) {
                                               String cur_sys_time = gcur_timeinfo.format(new Date(System.currentTimeMillis()));
                                               logfile_name = cur_sys_time + ".txt";
                                           }
                                           record_logfile(msg);
                                       } catch (Exception e) {
                                           e.printStackTrace();
                                       }
                                       b_need_recode = false;
                                   }
                               }
                           } catch (Exception e ){
                           e.printStackTrace();
                           }
                       }
                   };
                   Looper.loop();
               }

	    private class FileWrapper implements Comparable {
			private File file;
			public FileWrapper(File file) {
				this.file = file;
			}
			public int compareTo(Object obj) {
				assert obj instanceof FileWrapper;
				FileWrapper castObj = (FileWrapper)obj;
		
				if (this.file.getName().compareTo(castObj.getFile().getName()) > 0) {
					return 1;
				} else if (this.file.getName().compareTo(castObj.getFile().getName()) < 0) {
					return -1;
				} else {
					return 0;
				}
			}
			public File getFile() {
				return this.file;
			}
		}

		class MyTask extends java.util.TimerTask {
			public void run() {
				try {
					Log.e(TAG, "enter run :   ");
					if (record_time > 0) {
						create_new_filename();
						if (record_time % 4 == 0) {
							clean_logfile();
						}
					}
					record_time++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void create_new_filename(){
			String sarray[]=logfile_name.split(".txt");
			Log.e(TAG, sarray[0].toString());
			DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			Log.e(TAG, "line:"+Thread.currentThread().getStackTrace()[2].getLineNumber());
			try {
				Date d = format.parse(sarray[0].toString());
				Calendar c = Calendar.getInstance();
				c.setTime(d);
				c.add(c.MINUTE, 15);
				Date temp_date = c.getTime();
				System.out.println(format.format(temp_date));
				logfile_name = format.format(temp_date) + ".txt";
				Log.e(TAG,"new logfile_name"+logfile_name);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			File f = new File(RECORD_PATH+File.separator+logfile_name);
			try{
				if(!f.exists()){
					f.createNewFile();
				}
				Runtime.getRuntime().exec("chmod 666 " + RECORD_PATH+File.separator+logfile_name);
			}catch(java.io.IOException e){
				e.printStackTrace();
			}
			
		}

	    public int record_logfile(Message mMesg) 
			throws Exception {
			   /*String pname = mContext.getPackageName();
	            if (!pname.equals("com.shcmcc.diagnostic")) {
	                return 0;
	            }*/
	            //Log.e(TAG, "enter record_logfile:"+logfile_name);
				//Log.e(TAG, "msg.what:"+mMesg.what);
				//Log.e(TAG, "msg.arg1:"+mMesg.arg1);
				//Log.e(TAG, "msg.arg2:"+mMesg.arg2);
				if (mMesg.obj instanceof String){
					Log.i(TAG, "msg.obj: " + (String)mMesg.obj);
				}

	            String str_log = RECORD_FILE_BEGIN;
				int seg_num = 0;
				int seg_downloadtime = 0;
				switch(mMesg.what){
					case PRINT_LOG_CREATE_PLAYER:
					case PRINT_LOG_SETPLAYER_SOURCE:
					case PRINT_LOG_SET_VIDEOSURFACETEXTURE:
					case PRINT_LOG_PREPAREASYNC:
					case PRINT_LOG_START:
					case PRINT_LOG_PAUSE:
					case PRINT_LOG_SEEKTO:
					case PRINT_LOG_STOP:
					case PRINT_LOG_CURRENT_PLAYTIME:
					case PRINT_LOG_DESTORY_PLAYER:
						String mLog = (String)mMesg.obj;
						str_log += mLog;
						str_log += "\r\n";
						break;
					case MEDIA_SET_VIDEO_SIZE:
						str_log += "Video frame width:";
						str_log += mMesg.arg1;
						str_log += ",height:";
						str_log += mMesg.arg2;
						str_log += "\r\n";
						Log.e(TAG,"MEDIA_SET_VIDEO_SIZE:"+str_log);
						break;
					case PRINT_LOG_CODEC_INFO:
						String mCodec = new String(" ");
						if (mMesg.obj instanceof Parcel) {
							Parcel parcel = (Parcel) mMesg.obj;
							mCodec = parcel.readString();
							parcel.recycle();
							Log.e(TAG,"mCodec: "+mCodec);
							}
						if(mMesg.arg1 == MEDIA_INFO_AMLOGIC_VIDEO_CODEC){
							str_log += "Video codec:";
							str_log += mCodec;
							str_log += "\r\n";
						}else if(mMesg.arg1 == MEDIA_INFO_AMLOGIC_AUDIO_CODEC){
							str_log += "Audio codec:";
							str_log += mCodec;
							str_log += "\r\n";
						}
						break;
					case PRINT_LOG_PREPARED:
						str_log += "Playback is ready,spend time:";
						str_log += mMesg.arg1;
						str_log += "ms\r\n";
						break;
					case PRINT_LOG_FIRST_FRMAE_SHOWN:
						str_log += "show the first video frame,spend time:";
						str_log += mMesg.arg1;
						str_log += "ms\r\n";
						break;
					case MEDIA_SEEK_COMPLETE:
						str_log += "seek complete\r\n";
						break;
					case PRINT_LOG_START_DECODER:
						str_log += "start decoder\r\n";
						break;
					case PRINT_LOG_PLAYMODE:
						str_log += "play mode:";
						if(mMesg.arg1 == 0)
							str_log += "local";
						else if(mMesg.arg1 == 1)
							str_log += "live";
						else if(mMesg.arg1 == 2)
							str_log += "vod";
						str_log += ",duration:";
						str_log += mMesg.arg2;
						str_log += "\r\n";
						break;
					case MEDIA_INFO:
						if (mMesg.arg1 == MEDIA_INFO_HTTP_CONNECT_OK) {
							str_log += "Connect server OK! Spend time:";
							str_log += mMesg.arg2;
							str_log += "ms\r\n";
						} else if(mMesg.arg1 == MEDIA_INFO_HTTP_CONNECT_ERROR){
						    str_log += "Error happened,event:Http conect error\r\n";
						}else if (mMesg.arg1 == MEDIA_INFO_HTTP_CODE){
							str_log += "Http status code:";
							str_log += mMesg.arg2;
							str_log += "\r\n";
						} else if (mMesg.arg1 == MEDIA_INFO_HTTP_REDIRECT){
						    String mRedirectUrl = new String(" ");
							if (mMesg.obj instanceof Parcel) {
								Parcel parcel = (Parcel) mMesg.obj;
								mRedirectUrl = parcel.readString();
								parcel.recycle();
								Log.e(TAG,"mRedirectUrl: "+mRedirectUrl);
							}
							str_log += "Redirect url:";
							str_log += mRedirectUrl;
						} else if (mMesg.arg1 == MEDIA_INFO_LIVE_SHIFT){
						    str_log += "convert to ";
							if(mMesg.arg2 == 0)
								str_log += "live\r\n";
							else if(mMesg.arg2 == 1)
								str_log += "pltv\r\n";
						} else if (mMesg.arg1 == MEDIA_INFO_BUFFERING_START) {
						    str_log += "buffering start,need buffer data size:";	
							str_log += mMesg.arg2/(1024*1024)+"."+mMesg.arg2%(1024*1024);
							str_log += "MB\r\n";
						}else if (mMesg.arg1 == MEDIA_INFO_BUFFERING_END) {
						    if(mMesg.arg2 !=0){
								// in case buffering start not be send out
							    str_log += "buffering end,lasting time:";
								str_log += mMesg.arg2;
								str_log += "ms\r\n";
							}
						}
						break;
					case PRINT_LOG_TS_DOWNLOAD_BAGIN:
						str_log += "ts segment download is start,number:";
						
						if (mMesg.obj instanceof Parcel) {
							Parcel parcel = (Parcel) mMesg.obj;
							seg_num = parcel.readInt();
							parcel.recycle();
							Log.e(TAG,"seg_num: "+seg_num);
						}
						str_log += seg_num;
						
						str_log += ",filesize:";
						str_log += mMesg.arg1/(1024*1024);
						str_log += "MB,duration:";
						str_log += (mMesg.arg2/1000);
						str_log += "ms\r\n";
					break;
					case PRINT_LOG_TS_DOWNLOAD_COMPLETE:
						str_log += "ts segment download is complete,recvsize:";
						str_log += mMesg.arg1/(1024*1024);
						str_log += "MB,spend time:";
						
						if (mMesg.obj instanceof Parcel) {
							Parcel parcel = (Parcel) mMesg.obj;
							seg_downloadtime = parcel.readInt();
							parcel.recycle();
							Log.e(TAG,"seg_downloadtime: "+seg_downloadtime);
						}
						str_log += (seg_downloadtime/1000);
						str_log += "ms,rate:";
						str_log += mMesg.arg2;
						str_log += "bps\r\n";
					break;
					case PRINT_LOG_ERROR:
						str_log += "Error happened,event:";
						if(mMesg.arg1 >= 0 && mMesg.arg1 < player_error.length){
							str_log += player_error[mMesg.arg1];
							str_log += "\r\n";
						}
						Log.e(TAG,"PRINT_LOG_ERROR:"+str_log);
					break;
					case MEDIA_PLAYBACK_COMPLETE:
						str_log += "playback end\r\n";
					break;
				}
				
	            //start to write file

	            byte log_buf[] = str_log.getBytes();
	            Log.e(TAG, "====str_log: " + str_log);
	            //File fp = new File(RECORD_PATH + File.separator + logfile_name);
	            try {
                        OutputStream out = null;
                        //out = new FileOutputStream(fp,true);
                        out = new FileOutputStream(RECORD_PATH + File.separator + logfile_name,true);
                        //for (int i = 0; i < log_buf.length; i++){
                        //    out.write(log_buf[i]);
                        //}
                        out.write(log_buf);
                        out.close();
                   } catch (FileNotFoundException e) {
                       e.printStackTrace();
                   }catch(IOException e){
                       e.printStackTrace();
                   }
	            str_log = "";

	            //Runtime.getRuntime().exec("chmod 666 " + RECORD_PATH+File.separator+logfile_name);
                    Log.e(TAG, "*****str_log");
	            return 0;
	        }

		private void clean_logfile() {
			Log.e(TAG, "enter clean_logfile");
			File dirFile = new File(RECORD_PATH);
			File [] sortedFiles = listSortedFiles(dirFile);
			for (int i=0;i<sortedFiles.length-1;i++){
				if(sortedFiles[i].exists()) {
					boolean d = sortedFiles[i].delete();
				}
			}
		}
	
		private File[] listSortedFiles(File dirFile) {
			assert dirFile.isDirectory();
			File[] files = dirFile.listFiles();
			FileWrapper [] fileWrappers = new FileWrapper[files.length];
			for (int i=0; i<files.length; i++) {
				fileWrappers[i] = new FileWrapper(files[i]);
			}
		
			Arrays.sort(fileWrappers);
		
			File []sortedFiles = new File[files.length];
			for (int i=0; i<files.length; i++) {
				sortedFiles[i] = fileWrappers[i].getFile();
			}
		
			return sortedFiles;
		}

		private boolean getFirstFileNameInThisTask()
			throws Exception {
			File dirFile = new File(RECORD_PATH);
			File [] sortedFiles = listSortedFiles(dirFile);
			Log.e(TAG, "files number:"+sortedFiles.length);

			if(sortedFiles.length == 0){
				//SimpleDateFormat gcur_timeinfo = new SimpleDateFormat("yyyyMMddHHmmss");
				String cur_sys_time = gcur_timeinfo.format(new Date(System.currentTimeMillis()));
				logfile_name = cur_sys_time + ".txt";
				Log.e(TAG,"first logfile:"+logfile_name);
				File f = new File(RECORD_PATH+File.separator+logfile_name);
			    try{
				    if(!f.exists()){
					    f.createNewFile();
				    }
				    Runtime.getRuntime().exec("chmod 666 " + RECORD_PATH+File.separator+logfile_name);
			    }catch(java.io.IOException e){
				    e.printStackTrace();
			    }
				return true;
			} else {
			    String cur_sys_time = gcur_timeinfo.format(new Date(System.currentTimeMillis()));
				Log.e(TAG, "cur_sys_time:"+cur_sys_time);

				for (int i=0;i<sortedFiles.length;i++){
					if(sortedFiles[i].exists()) {
						String mExistFile = (sortedFiles[i].getName()).substring(0,14);
						Log.e(TAG, "mExistFile: "+mExistFile);
						int mTime = getDurationOfLogs(cur_sys_time,mExistFile);
						if(mTime < 0) {
							// mExistFile > cur_sys_time ,this is not possible,discard it
							Log.e(TAG, "discard this log!!!");
							return false;
 						} else if (mTime > 60*60*1000){
							sortedFiles[i].delete();
							Log.e(TAG, "delete:"+mExistFile);
						}else if (mTime > 15*60*1000){
							record_time ++;
							Log.e(TAG, "add record_time");
						}else {
							logfile_name = sortedFiles[i].getName();
							mFirstTime_interval = RECORD_TIME_INTERVAL - mTime;
							record_time ++;
							Log.e(TAG, "mFirstTime_interval:"+mFirstTime_interval);
							return true;
						}
					}
				}
				return false;
			}
		}

		private int getDurationOfLogs(String s1,String s2) 
		throws Exception { 

			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

			Date d1 = sdf.parse(s1); 
			Date d2 = sdf.parse(s2); 

			Log.e(TAG,"DateCompare,s1:"+s1+" d1:"+d1.getTime());
			Log.e(TAG,"DateCompare,s2:"+s2+" d2:"+d2.getTime());
			return (int)(d1.getTime()-d2.getTime());
	    } 

    }
}
