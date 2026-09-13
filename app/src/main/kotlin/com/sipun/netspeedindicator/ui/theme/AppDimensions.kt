package com.sipun.netspeedindicator.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.sipun.netspeedindicator.R

@Immutable
data class AppDimensions(
    val iconSize: Dp,
    val smallIconSize: Dp,
    val mediumIconSize: Dp,
    val vectorImageSize: Dp,
    val bottomBarHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val spaceBetween: Dp,
    val inputHeight: Dp,
    val buttonHeight: Dp,
    val cornerRadius: Dp,
    val contentPadding: PaddingValues
)

@Composable
fun rememberAppDimensions(): AppDimensions {
    val iconSize = dimensionResource(R.dimen.icon_size)
    val smallIconSize = dimensionResource(R.dimen.small_icon_size)
    val mediumIconSize = dimensionResource(R.dimen.medium_icon_size)
    val vectorImageSize = dimensionResource(R.dimen.vector_image_size)
    val bottomBarHeight = dimensionResource(R.dimen.bottom_bar_height)
    val horizontalPadding = dimensionResource(R.dimen.horizontal_padding)
    val verticalPadding = dimensionResource(R.dimen.vertical_padding)
    val spaceBetween = dimensionResource(R.dimen.space_between)
    val inputHeight = dimensionResource(R.dimen.input_height)
    val buttonHeight = dimensionResource(R.dimen.button_height)
    val cornerRadius = dimensionResource(R.dimen.corner_radius)

    return remember(iconSize, smallIconSize, mediumIconSize, vectorImageSize, bottomBarHeight, horizontalPadding, verticalPadding, spaceBetween, inputHeight, buttonHeight, cornerRadius) {
        AppDimensions(
            iconSize = iconSize,
            smallIconSize = smallIconSize,
            mediumIconSize = mediumIconSize,
            vectorImageSize = vectorImageSize,
            bottomBarHeight = bottomBarHeight,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            spaceBetween = spaceBetween,
            inputHeight = inputHeight,
            buttonHeight = buttonHeight,
            cornerRadius = cornerRadius,
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding)
        )
    }
}

val dimens: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current

val LocalDimensions = staticCompositionLocalOf<AppDimensions> { error("AppDimensions must be provided") }