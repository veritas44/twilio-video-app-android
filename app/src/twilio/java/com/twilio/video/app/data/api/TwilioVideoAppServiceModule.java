package com.twilio.video.app.data.api;

import com.twilio.video.app.ApplicationScope;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import retrofit2.Retrofit;

@Module(includes = VideoAppServiceModule.class)
public class TwilioVideoAppServiceModule {
  private static final String BASE_URL =
      "https://us-central1-video-app-79418.cloudfunctions.net/production/";

  @Provides
  @ApplicationScope
  VideoAppService providesVideoAppService(
      @Named("VideoAppService") Retrofit.Builder retrofitBuilder) {
    Retrofit retrofit = retrofitBuilder.baseUrl(BASE_URL).build();

    return retrofit.create(VideoAppService.class);
  }
}
