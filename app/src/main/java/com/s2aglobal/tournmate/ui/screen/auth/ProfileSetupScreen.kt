package com.s2aglobal.tournmate.ui.screen.auth

import com.s2aglobal.tournmate.ui.component.LightSystemBarIcons
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PanToolAlt
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.HomeRegionCountry
import com.s2aglobal.tournmate.domain.model.PlayerAvatar
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import com.s2aglobal.tournmate.ui.component.BottomSheetPicker
import com.s2aglobal.tournmate.ui.component.SportPickerRow
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.DarkNavy
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val SystemGray6 = Color(0xFFF2F2F7)
private const val MINIMUM_AGE = 13

private enum class PlayingHand(val displayName: String) {
    LEFT("LEFT HANDED"),
    RIGHT("RIGHT HANDED"),
}

private enum class ProfileSkillLevel(
    val displayName: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    BEGINNER("BEGINNER", "Just starting out, learning the basics.", Icons.Filled.Bolt),
    INTERMEDIATE("INTERMEDIATE", "Can rally and understand basic tactics.", Icons.Filled.SportsBasketball),
    ADVANCED("ADVANCED", "Tournament regular, consistent shots.", Icons.Outlined.EmojiEvents),
    PRO("PRO", "National level player or high-rank.", Icons.Filled.Verified),
}

private fun ageInYears(dob: Date): Int {
    val birth = Calendar.getInstance().apply { time = dob }
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    val beforeBirthday = now.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
        (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH) && now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))
    if (beforeBirthday) age -= 1
    return age
}

@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    onSignOut: () -> Unit,
    onComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(1) }
    var showSuccess by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var nameLoaded by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf<Gender?>(null) }
    var selectedDateOfBirth by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedSport by remember { mutableStateOf(CurrentSport.sport) }
    var selectedAvatar by remember { mutableStateOf(PlayerAvatar.SHUTTLECOCK) }
    var selectedCountry by remember { mutableStateOf(HomeRegionCountry.fallback) }
    var postalCode by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var playingHand by remember { mutableStateOf(PlayingHand.RIGHT) }
    var skillLevel by remember { mutableStateOf(ProfileSkillLevel.INTERMEDIATE) }

    LaunchedEffect(uiState.defaultName) {
        val defaultName = uiState.defaultName
        if (!nameLoaded && defaultName != null) {
            nameLoaded = true
            if (name.isEmpty()) name = defaultName
        }
    }

    val isUnderAge = selectedDateOfBirth?.let { ageInYears(it) < MINIMUM_AGE } ?: false
    val isFormValid = run {
        val nameOk = name.isNotBlank()
        val countryOk = RegionNormalizer.normalizeCountryCode(selectedCountry.code) != null
        val postalOk = postalCode.isBlank() ||
            RegionNormalizer.validateHomePostalForCountry(selectedCountry.code, postalCode) == null
        nameOk && countryOk && postalOk && selectedDateOfBirth != null && !isUnderAge
    }

    val goBack = { if (step > 1) step -= 1 else onSignOut() }
    BackHandler(enabled = !showSuccess) { goBack() }

    Crossfade(targetState = showSuccess, animationSpec = tween(400), label = "profileSuccess") { success ->
        if (success) {
            AllSetScreen(onContinue = onComplete)
            return@Crossfade
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuthBackButton(
                onClick = goBack,
                modifier = Modifier.padding(top = 16.dp).padding(horizontal = 24.dp),
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                modifier = Modifier.weight(1f),
                label = "profileStep",
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    when (currentStep) {
                        1 -> ProfileInfoStep(
                            name = name,
                            onNameChange = { name = it },
                            selectedGender = selectedGender,
                            onGenderToggle = { selectedGender = if (selectedGender == it) null else it },
                            selectedDateOfBirth = selectedDateOfBirth,
                            isUnderAge = isUnderAge,
                            onShowDatePicker = { showDatePicker = true },
                            selectedSport = selectedSport,
                            onSportChange = { selectedSport = it },
                            selectedAvatar = selectedAvatar,
                            onAvatarChange = { selectedAvatar = it },
                            selectedCountry = selectedCountry,
                            onShowCountryPicker = { showCountryPicker = true },
                            postalCode = postalCode,
                            onPostalCodeChange = { postalCode = it },
                        )
                        2 -> PlayingHandStep(selectedHand = playingHand, onHandChange = { playingHand = it })
                        3 -> SkillLevelStep(selectedLevel = skillLevel, onLevelChange = { skillLevel = it })
                    }
                }
            }

            if (uiState.error != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.error.orEmpty(), fontSize = 12.sp, color = Color.Red)
                }
            }

            Spacer(Modifier.height(16.dp))
            if (step < 3) {
                val enabled = step != 1 || isFormValid
                PrimaryPillButton(
                    onClick = { step += 1 },
                    enabled = enabled,
                    modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
                ) {
                    Text("NEXT STEP", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            } else {
                PrimaryPillButton(
                    onClick = {
                        viewModel.saveProfile(
                            name = name,
                            gender = selectedGender ?: Gender.PREFER_NOT_TO_SAY,
                            avatarId = selectedAvatar.id,
                            homeCountryCode = selectedCountry.code,
                            homePostalRaw = postalCode,
                            dateOfBirth = selectedDateOfBirth,
                            preferredSport = selectedSport,
                            playingHand = playingHand.name.lowercase(),
                            skillLevel = skillLevel.name.lowercase(),
                            onComplete = { showSuccess = true },
                        )
                    },
                    enabled = !uiState.isSaving,
                ) {
                    Text("CREATE PROFILE", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    if (uiState.isSaving) {
                        TrophySpinner(size = 18.dp, style = TrophySpinnerStyle.INLINE)
                    } else {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateOfBirthPicker(
            initial = selectedDateOfBirth,
            onConfirm = { selectedDateOfBirth = it; showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showCountryPicker) {
        BottomSheetPicker(
            title = "HOME COUNTRY",
            items = HomeRegionCountry.pickerOptions,
            selectedItem = selectedCountry,
            onItemSelected = { selectedCountry = it; showCountryPicker = false },
            onDismiss = { showCountryPicker = false },
            itemLabel = { it.name },
            itemSubtitle = { it.code },
            searchable = true,
            searchFilter = { item, query ->
                item.name.contains(query, ignoreCase = true) || item.code.contains(query, ignoreCase = true)
            },
        )
    }
}

// ── Step 1: Profile Info ─────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ProfileInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    selectedGender: Gender?,
    onGenderToggle: (Gender) -> Unit,
    selectedDateOfBirth: Date?,
    isUnderAge: Boolean,
    onShowDatePicker: () -> Unit,
    selectedSport: SportType,
    onSportChange: (SportType) -> Unit,
    selectedAvatar: PlayerAvatar,
    onAvatarChange: (PlayerAvatar) -> Unit,
    selectedCountry: HomeRegionCountry,
    onShowCountryPicker: () -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
) {
    StepHeader("Set Up\nYour Profile", "Tell us a bit about yourself.")
    Spacer(Modifier.height(24.dp))

    SectionLabel("FULL NAME")
    Spacer(Modifier.height(8.dp))
    FilledTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = "John Doe",
        icon = Icons.Outlined.Person,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
    Spacer(Modifier.height(24.dp))

    SectionLabel("GENDER", optional = true)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Gender.MALE to "MALE", Gender.FEMALE to "FEMALE").forEach { (g, label) ->
            val selected = selectedGender == g
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) Color.Black else SystemGray6)
                    .clickable { onGenderToggle(g) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    color = if (selected) Color.White else Color.Black,
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    SectionLabel("DATE OF BIRTH")
    Spacer(Modifier.height(8.dp))
    FilledRow(onClick = onShowDatePicker) {
        Icon(Icons.Outlined.CalendarMonth, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = selectedDateOfBirth?.let { formatDate(it) } ?: "Select your date of birth",
            fontSize = 15.sp,
            color = if (selectedDateOfBirth != null) Color.Black else Color.Gray,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.height(8.dp))
    if (isUnderAge) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = Color.Red, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("You must be at least $MINIMUM_AGE years old to use TournMate.", fontSize = 12.sp, color = Color.Red)
        }
    } else {
        Text("Required for age verification and tournament eligibility.", fontSize = 12.sp, color = Color.Gray)
    }
    Spacer(Modifier.height(24.dp))

    SectionLabel("YOUR SPORT")
    Spacer(Modifier.height(12.dp))
    SportPickerRow(selection = selectedSport, onSelect = onSportChange)
    Spacer(Modifier.height(24.dp))

    SectionLabel("AVATAR")
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SystemGray6, RoundedCornerShape(14.dp))
            .padding(10.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PlayerAvatar.selectable.forEach { avatar ->
            val isSelected = avatar == selectedAvatar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAvatarChange(avatar) },
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(avatar.avatarUrl(64)).crossfade(true).build(),
                        contentDescription = avatar.displayName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .then(if (isSelected) Modifier.border(2.dp, TournmatePurple, CircleShape) else Modifier),
                        contentScale = ContentScale.Crop,
                    )
                    if (isSelected) {
                        Box(
                            Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(18.dp)
                                .background(TournmatePurple, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    avatar.displayName,
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TournmatePurple else Color.Gray,
                )
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    SectionLabel("HOME COUNTRY")
    Spacer(Modifier.height(8.dp))
    FilledRow(onClick = onShowCountryPicker) {
        Icon(Icons.Outlined.Public, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(selectedCountry.name, fontSize = 15.sp, color = Color.Black)
            Text(selectedCountry.code, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.height(24.dp))

    SectionLabel("POSTAL / ZIP", optional = true)
    Spacer(Modifier.height(8.dp))
    FilledTextField(
        value = postalCode,
        onValueChange = onPostalCodeChange,
        placeholder = "75201",
        icon = Icons.Outlined.LocationOn,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
    )
    Spacer(Modifier.height(24.dp))

    Text(
        "You’ll see tournaments in your country. Postal / ZIP must match that country’s format (e.g. 5-digit US ZIP, 6-digit India PIN).",
        fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp,
    )
    Spacer(Modifier.height(24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthPicker(initial: Date?, onConfirm: (Date) -> Unit, onDismiss: () -> Unit) {
    val maxMillis = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { add(Calendar.YEAR, -5) }.timeInMillis
    }
    val draftMillis = remember(initial) {
        initial?.let {
            val local = Calendar.getInstance().apply { time = it }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        } ?: Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { add(Calendar.YEAR, -18) }.timeInMillis
    }
    val maxYear = remember { Calendar.getInstance().get(Calendar.YEAR) - 5 }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = draftMillis,
        yearRange = 1900..maxYear,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
            override fun isSelectableYear(year: Int) = year <= maxYear
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                    val local = Calendar.getInstance().apply {
                        clear()
                        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
                    }
                    onConfirm(local.time)
                } ?: onDismiss()
            }) {
                Text("CONFIRM", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TournmatePurple)
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    "DATE OF BIRTH",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

// ── Step 2: Playing Hand ─────────────────────────────────────────────────────

@Composable
private fun PlayingHandStep(selectedHand: PlayingHand, onHandChange: (PlayingHand) -> Unit) {
    StepHeader("Which is your\nplaying hand?", "This helps us match you for doubles.")
    Spacer(Modifier.height(32.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PlayingHand.entries.forEach { hand ->
            val isSelected = hand == selectedHand
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onHandChange(hand) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) TournmatePurple else Color.Gray.copy(alpha = 0.12f),
                ),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(
                                if (isSelected) TournmatePurple.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.06f),
                                RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Index-finger hand rotated to point sideways; mirrored for the left hand.
                        Icon(
                            Icons.Filled.PanToolAlt, null,
                            tint = if (isSelected) TournmatePurple else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(30.dp)
                                .scale(scaleX = if (hand == PlayingHand.LEFT) -1f else 1f, scaleY = 1f)
                                .rotate(90f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        hand.displayName,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                        color = if (isSelected) TournmatePurple else Color.Gray,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

// ── Step 3: Skill Level ──────────────────────────────────────────────────────

@Composable
private fun SkillLevelStep(selectedLevel: ProfileSkillLevel, onLevelChange: (ProfileSkillLevel) -> Unit) {
    StepHeader("What's your\nskill level?", "Be honest! It ensures fair matches.")
    Spacer(Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileSkillLevel.entries.forEach { level ->
            val isSelected = level == selectedLevel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onLevelChange(level) },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) TournmatePurple else Color.Gray.copy(alpha = 0.1f),
                ),
                color = Color.White,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(
                                if (isSelected) TournmatePurple.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.06f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            level.icon, null,
                            tint = if (isSelected) TournmatePurple else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            level.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                            color = if (isSelected) TournmatePurple else Color.Black,
                        )
                        Text(level.subtitle, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                    }
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, null, tint = TournmatePurple, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}

// ── Success: You're All Set! ─────────────────────────────────────────────────

@Composable
private fun AllSetScreen(onContinue: () -> Unit) {
    LightSystemBarIcons()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TournmatePurple, DarkNavy)))
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .size(88.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.15f))
                .background(Color.White, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = TournmatePurple, modifier = Modifier.size(44.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "You're All Set!",
            fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
            color = Color.White, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.85f))) { append("Welcome to the court,\n") }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("Champion") }
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.85f))) { append(". Your profile has\nbeen created successfully.") }
            },
            fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 50.dp)
                .height(58.dp),
            shape = RoundedCornerShape(29.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        ) {
            Text("GO TO DASHBOARD", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.5.sp, color = Color.Black)
        }
    }
}

// ── Shared Components ────────────────────────────────────────────────────────

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, color = Color.Black)
    Spacer(Modifier.height(10.dp))
    Text(subtitle, fontSize = 16.sp, color = Color.Gray)
}

@Composable
private fun SectionLabel(text: String, optional: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Black)
        if (optional) {
            Spacer(Modifier.width(4.dp))
            Text("(OPTIONAL)", fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun FilledRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SystemGray6)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun FilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.Gray, fontSize = 15.sp) },
        leadingIcon = { Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SystemGray6,
            unfocusedContainerColor = SystemGray6,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = TournmatePurple,
        ),
        keyboardOptions = keyboardOptions,
    )
}

@Composable
private fun PrimaryPillButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 34.dp)
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TournmatePurple,
            disabledContainerColor = TournmatePurple,
            disabledContentColor = Color.White,
        ),
    ) { content() }
}

private fun formatDate(date: Date): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
