package com.s2aglobal.tournmate.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import com.s2aglobal.tournmate.util.openInBrowser

private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)
private val GoogleBlue = Color(0xFF4285F4)
private val SystemGreen = Color(0xFF34C759)
private val SystemGray6 = Color(0xFFF2F2F7)
private val GoogleGBrush = Brush.linearGradient(listOf(GoogleBlue, GoogleRed, GoogleYellow, GoogleGreen))

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    initialCreateMode: Boolean = false,
    onSignInSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEmailSignUp by rememberSaveable { mutableStateOf(false) }
    var initialized by rememberSaveable { mutableStateOf(false) }

    if (!initialized) {
        initialized = true
        if (initialCreateMode && !uiState.isCreateAccount) {
            viewModel.toggleCreateAccount()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (showEmailSignUp) {
            BackHandler { showEmailSignUp = false; viewModel.clearError() }
            EmailCreateAccountScreen(
                viewModel = viewModel,
                onBack = { showEmailSignUp = false; viewModel.clearError() },
                onSuccess = onSignInSuccess,
            )
        } else if (uiState.isCreateAccount) {
            EliteAccessScreen(
                uiState = uiState,
                onBack = onBack,
                onGoogleSignIn = { viewModel.signInWithGoogle(context, onSignInSuccess) },
                onEmailSignUp = { viewModel.clearError(); showEmailSignUp = true },
                onToggleMode = { viewModel.toggleCreateAccount() },
            )
        } else {
            WelcomeBackScreen(
                viewModel = viewModel,
                uiState = uiState,
                onBack = onBack,
                onGoogleSignIn = { viewModel.signInWithGoogle(context, onSignInSuccess) },
                onSignIn = { viewModel.signInWithEmail(onSignInSuccess) },
                onToggleMode = { viewModel.toggleCreateAccount() },
            )
        }

        if (uiState.isLoading) {
            AuthLoadingOverlay(if (showEmailSignUp) "Creating account\u2026" else "Signing in\u2026")
        }
    }

    if (uiState.showResetPassword) {
        ResetPasswordSheet(viewModel = viewModel, uiState = uiState)
    }
}

@Composable
private fun EliteAccessScreen(
    uiState: LoginUiState,
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onEmailSignUp: () -> Unit,
    onToggleMode: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            AuthBackButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(28.dp))

            Text("Elite\nAccess.", fontSize = 48.sp, fontWeight = FontWeight.Black, lineHeight = 52.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Choose your preferred login\nmethod.", fontSize = 16.sp, color = Color.Gray)

            if (uiState.error != null) {
                ErrorBanner(uiState.error, Modifier.padding(top = 16.dp))
            }
            if (uiState.resetConfirmation != null) {
                SuccessBanner(uiState.resetConfirmation, Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            GoogleSignInButton(onClick = onGoogleSignIn, enabled = !uiState.isLoading)

            Spacer(modifier = Modifier.height(16.dp))
            DividerRow("OR USE EMAIL")
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onEmailSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Email, null, Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text("EMAIL SIGN UP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            val context = LocalContext.current
            Row {
                Text("By joining, you agree to our ", fontSize = 13.sp, color = Color.Gray)
                Text(
                    "Terms", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openInBrowser(context, "https://www.tournmate.com/terms") },
                )
                Text(" and ", fontSize = 13.sp, color = Color.Gray)
                Text(
                    "Privacy", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openInBrowser(context, "https://www.tournmate.com/privacy") },
                )
                Text(".", fontSize = 13.sp, color = Color.Gray)
            }
        }

        TextButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
            Row {
                Text("Already a member? ", fontSize = 14.sp, color = Color.Gray)
                Text("Sign In", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TournmatePurple)
            }
        }
    }
}

@Composable
private fun WelcomeBackScreen(
    viewModel: LoginViewModel,
    uiState: LoginUiState,
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignIn: () -> Unit,
    onToggleMode: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF2F2F7),
        unfocusedContainerColor = Color(0xFFF2F2F7),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            AuthBackButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(28.dp))

            Text("Welcome\nBack.", fontSize = 38.sp, fontWeight = FontWeight.Black, lineHeight = 42.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Great to see you again champion.", fontSize = 16.sp, color = Color.Gray)

            if (uiState.error != null) {
                ErrorBanner(uiState.error, Modifier.padding(top = 16.dp))
            }
            if (uiState.resetConfirmation != null) {
                SuccessBanner(uiState.resetConfirmation, Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text("EMAIL", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Email Address", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Email, null, Modifier.size(14.dp), tint = Color.Gray) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                TextButton(onClick = { viewModel.showResetPassword() }, modifier = Modifier.height(20.dp), contentPadding = PaddingValues(0.dp)) {
                    Text("FORGOT?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TournmatePurple, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Password", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = Color.Gray) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Outlined.Visibility, null, Modifier.size(16.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TournmatePurple,
                    contentColor = Color.White,
                    disabledContainerColor = TournmatePurple,
                    disabledContentColor = Color.White,
                ),
                enabled = !uiState.isLoading,
            ) {
                Text("SIGN IN", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))
            DividerRow("SOCIAL LOGIN")
            Spacer(modifier = Modifier.height(20.dp))

            GoogleSignInButton(onClick = onGoogleSignIn, enabled = !uiState.isLoading)
        }

        TextButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
            Row {
                Text("New here? ", fontSize = 14.sp, color = Color.Gray)
                Text("Create Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TournmatePurple)
            }
        }
    }
}

@Composable
private fun EmailCreateAccountScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF2F2F7),
        unfocusedContainerColor = Color(0xFFF2F2F7),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuthBackButton(onClick = onBack)
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(6.dp)
                        .background(TournmatePurple, RoundedCornerShape(3.dp)),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("Pro Identity", fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Enter your athlete credentials.", fontSize = 16.sp, color = Color.Gray)

            if (uiState.error != null) {
                ErrorBanner(uiState.error, Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text("FULL NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex. John Doe", color = Color.Gray) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text("EMAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your email", color = Color.Gray) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text("PASSWORD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("••••••••", color = Color.Gray) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Outlined.Visibility, null, Modifier.size(16.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )
        }

        Button(
            onClick = {
                viewModel.updateConfirmPassword(uiState.password)
                viewModel.createAccount(fullName, onSuccess)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 34.dp)
                .height(58.dp),
            shape = RoundedCornerShape(29.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TournmatePurple,
                contentColor = Color.White,
                disabledContainerColor = TournmatePurple,
                disabledContentColor = Color.White,
            ),
            enabled = !uiState.isLoading,
        ) {
            Text("CREATE ACCOUNT", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp))
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
        enabled = enabled,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "G",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(brush = GoogleGBrush),
            )
            Spacer(Modifier.width(10.dp))
            Text("Sign in with Google", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}

@Composable
private fun DividerRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Divider(Modifier.weight(1f), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(alpha = 0.4f),
            letterSpacing = 1.sp,
        )
        Divider(Modifier.weight(1f), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))
    }
}

@Composable
private fun ErrorBanner(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    StatusBanner(Icons.Filled.Warning, Color.Red, message, modifier)
}

@Composable
private fun SuccessBanner(message: String, modifier: Modifier = Modifier) {
    StatusBanner(Icons.Filled.CheckCircle, SystemGreen, message, modifier)
}

@Composable
private fun StatusBanner(icon: ImageVector, tint: Color, message: String, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, fontSize = 12.sp, color = Color.Black)
    }
}

/** 44dp circular chevron back button used across the auth screens (matches iOS). */
@Composable
internal fun AuthBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AuthLoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TrophySpinner(size = 44.dp)
            Text(message, fontSize = 15.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetPasswordSheet(viewModel: LoginViewModel, uiState: LoginUiState) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissResetPassword,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(TournmatePurple.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, null, tint = TournmatePurple, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SystemGray6)
                        .clickable(onClick = viewModel::dismissResetPassword),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Text(
                "Reset\nPassword", fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp,
                modifier = Modifier.padding(top = 12.dp).padding(horizontal = 24.dp),
            )
            Text(
                "Enter your email and we'll send a\npassword reset link to your inbox.",
                fontSize = 15.sp, color = Color.Gray, lineHeight = 21.sp,
                modifier = Modifier.padding(top = 8.dp).padding(horizontal = 24.dp),
            )

            TextField(
                value = uiState.resetEmail,
                onValueChange = viewModel::updateResetEmail,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).padding(horizontal = 24.dp),
                placeholder = { Text("Email Address", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Outlined.Email, null, Modifier.size(14.dp), tint = Color.Gray) },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SystemGray6,
                    unfocusedContainerColor = SystemGray6,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            )

            if (uiState.error != null) {
                Text(
                    uiState.error, fontSize = 12.sp, color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 24.dp),
                )
            }

            Button(
                onClick = viewModel::sendPasswordReset,
                enabled = !uiState.isSendingReset,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).padding(horizontal = 24.dp).height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TournmatePurple,
                    contentColor = Color.White,
                    disabledContainerColor = TournmatePurple,
                    disabledContentColor = Color.White,
                ),
            ) {
                if (uiState.isSendingReset) {
                    TrophySpinner(size = 18.dp, style = TrophySpinnerStyle.INLINE)
                } else {
                    Text("SEND RESET LINK", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                }
            }

            TextButton(
                onClick = viewModel::dismissResetPassword,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                Text("NEVERMIND, I GOT IT", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color.Gray)
            }
        }
    }
}
