package com.s2aglobal.tournmate.ui.screen.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.s2aglobal.tournmate.R
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.HomeRegionCountry
import com.s2aglobal.tournmate.domain.model.PlayerAvatar
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class PlayingHand(val displayName: String) {
    LEFT("LEFT HANDED"),
    RIGHT("RIGHT HANDED"),
}

private enum class ProfileSkillLevel(
    val displayName: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    BEGINNER("BEGINNER", "Just starting out, learning the basics.", Icons.Default.FlashOn),
    INTERMEDIATE("INTERMEDIATE", "Can rally and understand basic tactics.", Icons.Default.SportsMartialArts),
    ADVANCED("ADVANCED", "Tournament regular, consistent shots.", Icons.Default.Star),
    PRO("PRO", "National level player or high-rank.", Icons.Default.VerifiedUser),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    initialName: String = "",
    onBack: () -> Unit = {},
    onComplete: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf(initialName) }
    var selectedGender by remember { mutableStateOf<Gender?>(null) }
    var selectedDateOfBirth by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedSport by remember { mutableStateOf(SportType.BADMINTON) }
    var selectedAvatar by remember { mutableStateOf(PlayerAvatar.DEFAULT) }
    var selectedCountry by remember { mutableStateOf(HomeRegionCountry.fallback) }
    var postalCode by remember { mutableStateOf("") }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var playingHand by remember { mutableStateOf(PlayingHand.RIGHT) }
    var skillLevel by remember { mutableStateOf(ProfileSkillLevel.INTERMEDIATE) }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        label = "profileStep",
    ) { currentStep ->
        when (currentStep) {
            1 -> ProfileInfoStep(
                name = name,
                onNameChange = { name = it },
                selectedGender = selectedGender,
                onGenderChange = { selectedGender = it },
                selectedDateOfBirth = selectedDateOfBirth,
                onDateOfBirthChange = { selectedDateOfBirth = it },
                showDatePicker = showDatePicker,
                onShowDatePicker = { showDatePicker = it },
                selectedSport = selectedSport,
                onSportChange = { selectedSport = it },
                selectedAvatar = selectedAvatar,
                onAvatarChange = { selectedAvatar = it },
                selectedCountry = selectedCountry,
                onCountryChange = { selectedCountry = it },
                countryMenuExpanded = countryMenuExpanded,
                onCountryMenuToggle = { countryMenuExpanded = it },
                postalCode = postalCode,
                onPostalCodeChange = { postalCode = it },
                onBack = onBack,
                onNext = { step = 2 },
                isNextEnabled = name.length >= 2,
            )
            2 -> PlayingHandStep(
                selectedHand = playingHand,
                onHandChange = { playingHand = it },
                onBack = { step = 1 },
                onNext = { step = 3 },
            )
            3 -> SkillLevelStep(
                selectedLevel = skillLevel,
                onLevelChange = { skillLevel = it },
                onBack = { step = 2 },
                onCreateProfile = {
                    viewModel.saveProfile(
                        name = name,
                        gender = selectedGender ?: Gender.PREFER_NOT_TO_SAY,
                        avatarId = selectedAvatar.id,
                        homeCountryCode = selectedCountry.code,
                        homePostalCode = postalCode.ifBlank { null },
                        dateOfBirth = selectedDateOfBirth,
                        preferredSport = selectedSport,
                        onComplete = { step = 4 },
                    )
                },
            )
            4 -> AllSetScreen(onContinue = onComplete)
        }
    }
}

// ── Step 1: Profile Info ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    selectedGender: Gender?,
    onGenderChange: (Gender) -> Unit,
    selectedDateOfBirth: Date?,
    onDateOfBirthChange: (Date) -> Unit,
    showDatePicker: Boolean,
    onShowDatePicker: (Boolean) -> Unit,
    selectedSport: SportType,
    onSportChange: (SportType) -> Unit,
    selectedAvatar: PlayerAvatar,
    onAvatarChange: (PlayerAvatar) -> Unit,
    selectedCountry: HomeRegionCountry,
    onCountryChange: (HomeRegionCountry) -> Unit,
    countryMenuExpanded: Boolean,
    onCountryMenuToggle: (Boolean) -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text("Create\nAccount", fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Sign up to start your pro journey.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(28.dp))

            SectionLabel("FULL NAME")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = onNameChange,
                placeholder = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray, focusedBorderColor = BrandPurple),
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("GENDER (OPTIONAL)")
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenderButton("MALE", selectedGender == Gender.MALE, { onGenderChange(Gender.MALE) }, Modifier.weight(1f))
                GenderButton("FEMALE", selectedGender == Gender.FEMALE, { onGenderChange(Gender.FEMALE) }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("DATE OF BIRTH")
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onShowDatePicker(true) },
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.LightGray),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = selectedDateOfBirth?.let { formatDate(it) } ?: "Select your date of birth",
                        color = if (selectedDateOfBirth != null) Color.Black else Color.Gray,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                }
            }
            Text("Required for age verification and tournament eligibility.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("Your Sport")
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SportType.SELECTABLE.forEach { sport ->
                    SportCard(sport, selectedSport == sport, { onSportChange(sport) }, Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("AVATAR")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlayerAvatar.selectable.forEach { avatar ->
                    val isSelected = avatar == selectedAvatar
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(avatar.avatarUrl(64)).crossfade(true).build(),
                            contentDescription = avatar.displayName,
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .then(if (isSelected) Modifier.border(3.dp, BrandPurple, CircleShape) else Modifier.border(1.dp, Color.LightGray, CircleShape))
                                .clickable { onAvatarChange(avatar) },
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(avatar.displayName, style = MaterialTheme.typography.labelSmall, color = if (isSelected) BrandPurple else Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("HOME COUNTRY")
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onCountryMenuToggle(true) },
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color.LightGray),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83C\uDF10", fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selectedCountry.name, fontWeight = FontWeight.Medium)
                            Text(selectedCountry.code, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text("\u25BE", fontSize = 16.sp, color = Color.Gray)
                    }
                }
                DropdownMenu(expanded = countryMenuExpanded, onDismissRequest = { onCountryMenuToggle(false) }) {
                    HomeRegionCountry.pickerOptions.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.name} (${country.code})") },
                            onClick = { onCountryChange(country); onCountryMenuToggle(false) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("POSTAL / ZIP (OPTIONAL)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = postalCode, onValueChange = onPostalCodeChange,
                placeholder = { Text("75201") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray, focusedBorderColor = BrandPurple),
            )
            Text(
                "You'll see tournaments in your country. Postal / ZIP must match that country's format (e.g. 5-digit US ZIP, 6-digit India PIN).",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                enabled = isNextEnabled,
            ) {
                Text("NEXT STEP  \u203A", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateOfBirth?.time
                ?: Calendar.getInstance().apply { add(Calendar.YEAR, -20) }.timeInMillis,
        )
        DatePickerDialog(
            onDismissRequest = { onShowDatePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = millis
                        val localCal = Calendar.getInstance()
                        localCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                        localCal.set(Calendar.MILLISECOND, 0)
                        onDateOfBirthChange(localCal.time)
                    }
                    onShowDatePicker(false)
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { onShowDatePicker(false) }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }
}

// ── Step 2: Playing Hand ─────────────────────────────────────────────────────

@Composable
private fun PlayingHandStep(
    selectedHand: PlayingHand,
    onHandChange: (PlayingHand) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding(),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Which is your\nplaying hand?", fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("This helps us match you for doubles.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(40.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayingHand.entries.forEach { hand ->
                    val isSelected = hand == selectedHand
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onHandChange(hand) },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandPurple else Color.LightGray),
                        color = Color.White,
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) BrandPurple.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (hand == PlayingHand.LEFT) "\uD83E\uDD1A" else "\uD83D\uDC49",
                                        fontSize = 28.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                hand.displayName,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (isSelected) BrandPurple else Color.Gray,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            ) {
                Text("NEXT STEP  \u203A", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Step 3: Skill Level ──────────────────────────────────────────────────────

@Composable
private fun SkillLevelStep(
    selectedLevel: ProfileSkillLevel,
    onLevelChange: (ProfileSkillLevel) -> Unit,
    onBack: () -> Unit,
    onCreateProfile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding(),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("What's your\nskill level?", fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Be honest! It ensures fair matches.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            ProfileSkillLevel.entries.forEach { level ->
                val isSelected = level == selectedLevel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onLevelChange(level) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandPurple else Color.LightGray),
                    color = Color.White,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BrandPurple.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(level.icon, null, tint = if (isSelected) BrandPurple else Color.Gray, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(level.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(level.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null, tint = BrandPurple, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onCreateProfile, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            ) {
                Text("CREATE PROFILE  \u2714", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Success: You're All Set! ─────────────────────────────────────────────────

@Composable
private fun AllSetScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0533), Color(0xFF4A148C)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = BrandPurple,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "You're All Set!",
                fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = Color.White, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Welcome to the court,\nChampion. Your profile has\nbeen created successfully.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        ) {
            Text("GO TO DASHBOARD", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        }
    }
}

// ── Shared Components ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, letterSpacing = 0.5.sp)
}

@Composable
private fun GenderButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) BrandPurple.copy(alpha = 0.08f) else Color.Transparent,
            contentColor = if (isSelected) BrandPurple else Color.DarkGray,
        ),
        border = BorderStroke(1.dp, if (isSelected) BrandPurple else Color.LightGray),
    ) {
        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun SportCard(sport: SportType, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconRes = when (sport) {
        SportType.BADMINTON -> R.drawable.ic_badminton
        SportType.PICKLEBALL -> R.drawable.ic_figure_badminton
        SportType.TENNIS -> R.drawable.ic_sportscourt
        else -> R.drawable.ic_badminton
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandPurple else Color.LightGray),
        color = if (isSelected) BrandPurple.copy(alpha = 0.05f) else Color.White,
    ) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(id = iconRes), sport.displayName, Modifier.size(32.dp), tint = if (isSelected) BrandPurple else Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(sport.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) BrandPurple else Color.DarkGray)
        }
    }
}

private fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(date)
}
