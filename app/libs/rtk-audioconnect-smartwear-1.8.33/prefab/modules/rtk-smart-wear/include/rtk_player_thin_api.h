/*
 * Copyright (c) 2026. Realtek Semiconductor Corporation.
 *
 * SPDX-License-Identifier: LicenseRef-Realtek-5-Clause
 */

#ifndef AIGLASS_RTK_PLAYER_THIN_API_H
#define AIGLASS_RTK_PLAYER_THIN_API_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

struct RTKPlayer;

// Maximum number of planes in the returned video frames,
// Unused planes should be set to NULL and linesize is 0;
#define VIDEO_FRAME_MAX_PLANE_NUM 8

/**
 * @brief Image pixel format.
 */
typedef enum
{
    kImageFormatYUV420P = 0,
    kImageFormatRGBA,
    kImageFormatRGB24,
} ImageFormat;

/**
 * @brief Basic information describing a single video frame.
 */
typedef struct VideoFrame
{
    /**
     * @brief The width of the VideoFrame.
     */
    int width;

    /**
     * @brief The height of the VideoFrame.
     */
    int height;

    /**
     * @brief The display timestamps of the VideoFrame.
     */
    int64_t pts;

    /**
     * @brief The pixel format of the VideoFrame.
     */
    ImageFormat format;

    /**
     * @brief The data content of the VideoFrame.
     */
    uint8_t *data[VIDEO_FRAME_MAX_PLANE_NUM];

    /**
     * @brief Number of bytes per row in a single plane.
     */
    int linesize[VIDEO_FRAME_MAX_PLANE_NUM];
} VideoFrame;

/**
 * @note The VideoFrame object and its data pointers are only valid during the callback.
 * If asynchronous use is needed, the receiver must copy the data.
 */
typedef void(*VideoFrameCallback)(const VideoFrame *frame, void *user_data);

/**
 * @brief Create player instance.
 *
 * @return Newly created player instance.
 */
extern RTKPlayer *rtk_player_create(void);

/**
 * @brief Set a callback to receive the decoded video frame.
 *
 * @param[in] player Player instance already created.
 * @param[in] format Desired video frame format, See enum \ref ImageFormat.
 * @param[in] callback Callback to be registered, which is a function pointer.
 * @param[in] user_data The user input context.
 */
extern void rtk_player_set_video_frame_callback(RTKPlayer *player,
                                                ImageFormat format,
                                                VideoFrameCallback callback,
                                                void *user_data);

/**
 * Remove registered video frames callback.
 *
 * @param player Player instance already created.
 */
extern void rtk_player_remove_video_frame_callback(RTKPlayer *player);

/**
 * @brief Destroy the created player instance.
 *
 * @param[in] player Player instance already created.
 */
extern void rtk_player_destroy(RTKPlayer *player);

#ifdef __cplusplus
}
#endif

#endif //AIGLASS_RTK_PLAYER_THIN_API_H