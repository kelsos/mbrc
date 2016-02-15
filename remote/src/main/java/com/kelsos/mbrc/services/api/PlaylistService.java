package com.kelsos.mbrc.services.api;

import com.kelsos.mbrc.dto.BaseResponse;
import com.kelsos.mbrc.dto.PaginatedResponse;
import com.kelsos.mbrc.dto.playlist.PlaylistDto;
import com.kelsos.mbrc.dto.playlist.PlaylistTrack;
import com.kelsos.mbrc.dto.playlist.PlaylistTrackInfo;
import com.kelsos.mbrc.dto.requests.MoveRequest;
import com.kelsos.mbrc.dto.requests.PlayPathRequest;
import com.kelsos.mbrc.dto.requests.PlaylistRequest;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface PlaylistService {

  @PUT("/playlists/play")
  Observable<BaseResponse> playPlaylist(@Body PlayPathRequest body);

  @PUT("/playlists/{id}/tracks/move")
  Observable<BaseResponse> playlistMoveTrack(@Path("id") int id, @Body MoveRequest body);

  @GET("/playlists/{id}/tracks")
  Observable<PaginatedResponse<PlaylistTrack>> getPlaylistTracks(@Path("id") long id, @Query("offset") int offset,
      @Query("limit") int limit, @Query("after") long after);

  @DELETE("/playlists/{id}/tracks")
  Observable<BaseResponse> deletePlaylistTrack(@Path("id") int id, @Query("index") int index);

  @DELETE("/playlists/{id}")
  Observable<BaseResponse> deletePlaylist(@Path("id") int id);

  @PUT("/playlists")
  Observable<BaseResponse> createPlaylist(@Body PlaylistRequest body);

  @GET("/playlists")
  Observable<PaginatedResponse<PlaylistDto>> getPlaylists(@Query("offset") int offset, @Query("limit") int limit,
      @Query("after") long after);

  @PUT("/playlists/{id}/tracks")
  Observable<BaseResponse> addTracksToPlaylist(@Path("id") int id, @Body PlaylistRequest body);

  @GET("/playlists/trackinfo")
  Observable<PaginatedResponse<PlaylistTrackInfo>> getPlaylistTrackInfo(@Query("offset") int offset,
      @Query("limit") int limit,
      @Query("after") long after);
}