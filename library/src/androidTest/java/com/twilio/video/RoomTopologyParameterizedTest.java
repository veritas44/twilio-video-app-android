/*
 * Copyright (C) 2017 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twilio.video;

import static com.twilio.video.util.VideoAssert.assertIsParticipantSid;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import com.kevinmost.junit_retry_rule.Retry;
import com.kevinmost.junit_retry_rule.RetryRule;
import com.twilio.video.base.BaseVideoTest;
import com.twilio.video.helper.CallbackHelper;
import com.twilio.video.test.BuildConfig;
import com.twilio.video.ui.MediaTestActivity;
import com.twilio.video.util.Constants;
import com.twilio.video.util.CredentialsUtils;
import com.twilio.video.util.FakeVideoCapturer;
import com.twilio.video.util.RoomUtils;
import com.twilio.video.util.Topology;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@LargeTest
public class RoomTopologyParameterizedTest extends BaseVideoTest {
  @Parameterized.Parameters(name = "Topology: {0}, enableRecording: {1}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {Topology.P2P, false},
          {Topology.GROUP, false},
          {Topology.GROUP, true}
        });
  }

  @Rule
  public GrantPermissionRule recordAudioPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO);

  @Rule
  public ActivityTestRule<MediaTestActivity> activityRule =
      new ActivityTestRule<>(MediaTestActivity.class);

  @Rule public final RetryRule retryRule = new RetryRule();

  private MediaTestActivity mediaTestActivity;
  private String identity;
  private String token;
  private String roomName;
  private final CallbackHelper.FakeRoomListener roomListener =
      new CallbackHelper.FakeRoomListener();
  private Room room;
  private LocalAudioTrack localAudioTrack;
  private LocalVideoTrack localVideoTrack;
  private final Topology topology;
  private final boolean enableRecording;

  public RoomTopologyParameterizedTest(Topology topology, boolean enableRecording) {
    this.topology = topology;
    this.enableRecording = enableRecording;
  }

  @Before
  public void setup() throws InterruptedException {
    super.setup();
    mediaTestActivity = activityRule.getActivity();
    identity = Constants.PARTICIPANT_ALICE;
    roomName = random(Constants.ROOM_NAME_LENGTH);
    assertNotNull(RoomUtils.createRoom(roomName, topology, enableRecording));
    token = CredentialsUtils.getAccessToken(identity, topology);
  }

  @After
  public void teardown() throws InterruptedException {
    if (room != null && room.getState() != RoomState.DISCONNECTED) {
      roomListener.onDisconnectedLatch = new CountDownLatch(1);
      room.disconnect();
      assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    }
    if (localAudioTrack != null) {
      localAudioTrack.release();
    }
    if (localVideoTrack != null) {
      localVideoTrack.release();
    }
    assertTrue(MediaFactory.isReleased());
  }

  @Test
  public void shouldReturnLocalParticipantOnConnected() throws InterruptedException {
    roomListener.onConnectedLatch = new CountDownLatch(1);
    roomListener.onDisconnectedLatch = new CountDownLatch(1);
    localAudioTrack = LocalAudioTrack.create(mediaTestActivity, true);
    localVideoTrack = LocalVideoTrack.create(mediaTestActivity, true, new FakeVideoCapturer());

    ConnectOptions connectOptions =
        new ConnectOptions.Builder(token)
            .roomName(roomName)
            .audioTracks(Collections.singletonList(localAudioTrack))
            .videoTracks(Collections.singletonList(localVideoTrack))
            .build();
    room = Video.connect(mediaTestActivity, connectOptions, roomListener);
    assertNull(room.getLocalParticipant());
    assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));

    LocalParticipant localParticipant = room.getLocalParticipant();
    assertNotNull(localParticipant);
    assertEquals(identity, localParticipant.getIdentity());
    assertEquals(localAudioTrack, localParticipant.getAudioTracks().get(0).getAudioTrack());
    assertEquals(1, localParticipant.getLocalAudioTracks().size());
    assertEquals(
        localAudioTrack, localParticipant.getLocalAudioTracks().get(0).getLocalAudioTrack());
    assertEquals(localVideoTrack, localParticipant.getVideoTracks().get(0).getVideoTrack());
    assertEquals(1, localParticipant.getLocalVideoTracks().size());
    assertEquals(
        localVideoTrack, localParticipant.getLocalVideoTracks().get(0).getLocalVideoTrack());
    assertNotNull(localParticipant.getSid());
    assertTrue(!localParticipant.getSid().isEmpty());
    assertIsParticipantSid(localParticipant.getSid());
  }

  @Test
  @Retry(times = BuildConfig.MAX_TEST_RETRIES)
  public void shouldReconnect() throws InterruptedException {
    ConnectOptions connectOptions = new ConnectOptions.Builder(token).roomName(roomName).build();
    for (int i = 0; i < 5; i++) {
      roomListener.onConnectedLatch = new CountDownLatch(1);
      roomListener.onDisconnectedLatch = new CountDownLatch(1);

      room = Video.connect(mediaTestActivity, connectOptions, roomListener);
      assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));
      assertEquals(RoomState.CONNECTED, room.getState());

      room.disconnect();
      assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
      assertEquals(RoomState.DISCONNECTED, room.getState());

      Thread.sleep(1000);
    }
  }

  @Test
  public void shouldFailToConnectWithInvalidToken() throws InterruptedException {
    String invalidToken = "invalid token";
    ConnectOptions connectOptions = new ConnectOptions.Builder(invalidToken).build();
    final CountDownLatch connectFailure = new CountDownLatch(1);
    Video.connect(
        mediaTestActivity,
        connectOptions,
        new Room.Listener() {
          @Override
          public void onConnected(Room room) {
            fail();
          }

          @Override
          public void onConnectFailure(Room room, TwilioException twilioException) {
            assertEquals(TwilioException.ACCESS_TOKEN_INVALID_EXCEPTION, twilioException.getCode());
            connectFailure.countDown();
          }

          @Override
          public void onDisconnected(Room room, TwilioException twilioException) {
            fail();
          }

          @Override
          public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
            fail();
          }

          @Override
          public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
            fail();
          }

          @Override
          public void onRecordingStarted(Room room) {
            fail();
          }

          @Override
          public void onRecordingStopped(Room room) {
            fail();
          }
        });
    assertTrue(connectFailure.await(10, TimeUnit.SECONDS));
  }

  @Test
  public void shouldReturnValidRecordingState() throws InterruptedException {
    roomListener.onConnectedLatch = new CountDownLatch(1);
    roomListener.onDisconnectedLatch = new CountDownLatch(1);

    ConnectOptions connectOptions = new ConnectOptions.Builder(token).roomName(roomName).build();
    room = Video.connect(mediaTestActivity, connectOptions, roomListener);
    assertNull(room.getLocalParticipant());
    assertTrue(roomListener.onConnectedLatch.await(20, TimeUnit.SECONDS));

    if (topology == Topology.GROUP && enableRecording) {
      Assert.assertTrue(room.isRecording());
    } else {
      assertFalse(room.isRecording());
    }

    room.disconnect();

    // Wait for disconnect and validate recording state
    assertTrue(roomListener.onDisconnectedLatch.await(20, TimeUnit.SECONDS));
    Assert.assertFalse(room.isRecording());
  }

  @Test
  public void shouldDisconnectDuplicateParticipant() throws InterruptedException {
    // Connect first bobRemoteParticipant
    ConnectOptions connectOptions = new ConnectOptions.Builder(token).roomName(roomName).build();
    final CountDownLatch connectedLatch = new CountDownLatch(1);
    final CountDownLatch disconnectedLatch = new CountDownLatch(1);
    Video.connect(
        mediaTestActivity,
        connectOptions,
        new Room.Listener() {
          @Override
          public void onConnected(Room room) {
            connectedLatch.countDown();
          }

          @Override
          public void onConnectFailure(Room room, TwilioException twilioException) {
            fail();
          }

          @Override
          public void onDisconnected(Room room, TwilioException twilioException) {
            assertEquals(
                TwilioException.PARTICIPANT_DUPLICATE_IDENTITY_EXCEPTION,
                twilioException.getCode());
            disconnectedLatch.countDown();
          }

          @Override
          public void onParticipantConnected(Room room, RemoteParticipant participant) {
            fail();
          }

          @Override
          public void onParticipantDisconnected(Room room, RemoteParticipant participant) {
            fail();
          }

          @Override
          public void onRecordingStarted(Room room) {
            fail();
          }

          @Override
          public void onRecordingStopped(Room room) {
            fail();
          }
        });
    assertTrue(connectedLatch.await(10, TimeUnit.SECONDS));

    // Connect second bobRemoteParticipant
    connectOptions = new ConnectOptions.Builder(token).roomName(roomName).build();
    roomListener.onConnectedLatch = new CountDownLatch(1);
    roomListener.onDisconnectedLatch = new CountDownLatch(1);
    room = Video.connect(mediaTestActivity, connectOptions, roomListener);
    assertTrue(roomListener.onConnectedLatch.await(10, TimeUnit.SECONDS));

    // First bobRemoteParticipant should get disconnected
    assertTrue(disconnectedLatch.await(10, TimeUnit.SECONDS));
  }
}
