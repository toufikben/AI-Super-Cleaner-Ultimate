package com.aisupercleaner.ultimate.presentation.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Rounded.CleaningServices, R.string.onboarding_1_title, R.string.onboarding_1_desc),
        OnboardingPage(Icons.Rounded.Speed, R.string.onboarding_2_title, R.string.onboarding_2_desc),
        OnboardingPage(Icons.Rounded.Lock, R.string.onboarding_3_title, R.string.onboarding_3_desc),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(colors.background).padding(24.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            if (pagerState.currentPage < pages.lastIndex) {
                TextButton(onClick = onFinished) { Text(stringResource(R.string.skip)) }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { index ->
            val page = pages[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    Modifier.size(160.dp).clip(CircleShape).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(80.dp))
                }
                Spacer(Modifier.height(40.dp))
                Text(stringResource(page.titleRes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = colors.onBackground)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(page.descriptionRes), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            pages.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val width by animateFloatAsState(targetValue = if (selected) 28f else 8f, label = "ind")
                Box(
                    Modifier.padding(horizontal = 4.dp).height(8.dp).width(width.dp).clip(CircleShape)
                        .background(if (selected) colors.primary else colors.outlineVariant)
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                stringResource(if (pagerState.currentPage == pages.lastIndex) R.string.get_started else R.string.next),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
