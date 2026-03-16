// Nalazi se u: com/ogaivirt/triviago/ui/common/ProfileImage.kt

package com.ogaivirt.triviago.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ogaivirt.triviago.R

@Composable
fun ProfileImage(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {

    val imageSource = if (imageUrl.isNullOrBlank()) {
        R.drawable.ic_default_profile
    } else {
        imageUrl
    }

    AsyncImage(
        model = imageSource,
        contentDescription = "Profilna slika",
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.ic_default_profile),
        error = painterResource(id = R.drawable.ic_default_profile)
    )
}