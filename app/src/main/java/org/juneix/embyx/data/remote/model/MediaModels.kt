package com.lalakiop.embyx.data.remote.model

import com.google.gson.annotations.SerializedName

data class ItemsResponse(
    @SerializedName("Items") val items: List<ItemDto>?
)

data class ViewsResponse(
    @SerializedName("Items") val items: List<ViewDto>?
)

data class ViewDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String?,
    @SerializedName("CollectionType") val collectionType: String?
)

data class ItemDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String?,
    @SerializedName("Overview") val overview: String?,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long?,
    @SerializedName("ImageTags") val imageTags: ImageTags?,
    @SerializedName("UserData") val userData: UserDataDto?
)

data class ImageTags(
    @SerializedName("Primary") val primary: String?
)

data class UserDataDto(
    @SerializedName("IsFavorite") val isFavorite: Boolean?
)

data class PlaybackInfoResponse(
    @SerializedName("MediaSources") val mediaSources: List<MediaSourceDto>?
)

data class MediaSourceDto(
    @SerializedName("Id") val id: String?,
    @SerializedName("Name") val name: String?,
    @SerializedName("Width") val width: Int?,
    @SerializedName("Height") val height: Int?,
    @SerializedName("Bitrate") val bitrate: Int?,
    @SerializedName("Path") val path: String?,
    @SerializedName("DirectStreamUrl") val directStreamUrl: String?,
    @SerializedName("TranscodingUrl") val transcodingUrl: String?
)

data class PlaybackInfoRequest(
    @SerializedName("DeviceProfile") val deviceProfile: DeviceProfileRequest
)

data class DeviceProfileRequest(
    @SerializedName("MaxStaticBitrate") val maxStaticBitrate: Int,
    @SerializedName("MaxStreamingBitrate") val maxStreamingBitrate: Int,
    @SerializedName("MusicStreamingTranscodingBitrate") val musicStreamingTranscodingBitrate: Int,
    @SerializedName("DirectPlayProfiles") val directPlayProfiles: List<DirectPlayProfileRequest>,
    @SerializedName("TranscodingProfiles") val transcodingProfiles: List<TranscodingProfileRequest>,
    @SerializedName("CodecProfiles") val codecProfiles: List<CodecProfileRequest>,
    @SerializedName("SubtitleProfiles") val subtitleProfiles: List<SubtitleProfileRequest>,
    @SerializedName("ResponseProfiles") val responseProfiles: List<ResponseProfileRequest>
)

data class DirectPlayProfileRequest(
    @SerializedName("Container") val container: String,
    @SerializedName("Type") val type: String,
    @SerializedName("VideoCodec") val videoCodec: String,
    @SerializedName("AudioCodec") val audioCodec: String
)

data class TranscodingProfileRequest(
    @SerializedName("Container") val container: String,
    @SerializedName("Type") val type: String,
    @SerializedName("AudioCodec") val audioCodec: String,
    @SerializedName("VideoCodec") val videoCodec: String,
    @SerializedName("Context") val context: String,
    @SerializedName("Protocol") val protocol: String
)

data class CodecProfileRequest(
    @SerializedName("Type") val type: String,
    @SerializedName("Codec") val codec: String,
    @SerializedName("Conditions") val conditions: List<CodecConditionRequest>
)

data class CodecConditionRequest(
    @SerializedName("Condition") val condition: String,
    @SerializedName("Property") val property: String,
    @SerializedName("Value") val value: String
)

data class SubtitleProfileRequest(
    @SerializedName("Format") val format: String,
    @SerializedName("Method") val method: String
)

data class ResponseProfileRequest(
    @SerializedName("Type") val type: String,
    @SerializedName("Container") val container: String,
    @SerializedName("MimeType") val mimeType: String
)
