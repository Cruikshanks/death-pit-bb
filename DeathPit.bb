;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; DEATH PIT
;;
;; Based on an idea from a Biltz Basic tutorial example
;; Modified (a lot!) by 'Feersum Endjin'
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


AppTitle "Death Pit"

;Constants
Const LEVEL1# = 0.05, LEVEL2# = 0.1, LEVEL3# = 0.15
Const WIDTH = 640, HEIGHT = 480, PANEL_HEIGHT = 40
Const START_SPEED = 1.0
Const SIZE_OF_TROLLS = 15, SIZE_OF_COINS = 15, SIZE_OF_PLAYER = 15
Const NUM_OF_TROLLS = 20, NUM_OF_COINS = 20, NUM_OF_LIVES = 2, NUM_OF_HSCORES = 10
Const MAX_TIME = 60 * 1000
Const HSCORE_FILE$ = "prefs"

;Types
Type playerData
	Field x#
	Field y#
	Field livesLeft
	Field currentLevel
	Field score
	Field speed#
	Field dir
End Type

Type coin
	Field x
	Field y
End Type

Type troll
	Field x
	Field y
End Type

Type highscore
	Field name$
	Field score
End Type

;this puts us in graphics mode x,y,colour depth, mode
;x and y are obvious
;colour depth = bits 16,24 or 32bit
;mode = 0,1,2,3 = auto,fullscreen,windowed,scaledwindow
Graphics WIDTH,HEIGHT
SetBuffer BackBuffer()

;Load Font
Global arialFnt = LoadFont("Arial", 18, True, False, False)
	SetFont arialFnt

Cls
Text 20,40,"Loading..."
Flip

;Global Variables
Global status = 0
Global countdown = 0, curTime = 0
Global player.playerData = New playerData

;Load Sounds
Global loadingSound = LoadSound("Sounds\Loading.wav")
Global themeSound = LoadSound("Sounds\Theme.mp3")
Global deathSound = LoadSound("Sounds\Laugh.wav")
Global winSound = LoadSound("Sounds\Win.mp3")
Global beepSound = LoadSound("Sounds\Ping.wav")
Global lvlStartSound = LoadSound("Sounds\Start.wav")
Global killedSound = LoadSound("Sounds\Scream.wav")
Global bgChannel,fgChannel
	;check sounds loaded
	If loadingSound = 0 Or themeSound = 0 Or deathSound = 0 Or winSound = 0 Or beepSound = 0 Or lvlStartSound = 0 Or killedSound = 0
		error1$ = "A sound did not load"
		error2$ = "Please ensure you have not moved/modfied Death Pit's 'sounds' directory"
		Error(error1,error2)
		;Free up memory used for storing font
		FreeFont arialFnt
		End
	EndIf

;Load Images
Global loadingImg = LoadImage("Graphics\DPStartup.bmp")
Global titleImg = LoadImage("graphics\DeathPitTitle.bmp")
Global infoImg = LoadImage("graphics\DeathPitInfo.bmp")
Global deathImg = LoadImage("graphics\DeathPitDead.bmp")
Global winImg = LoadImage("graphics\DeathPitWin.bmp")
Global heroImg = LoadImage("graphics\DeathPitHero.bmp")
Global coinImg = LoadImage("graphics\DeathPitCoin.bmp")
Global trollImg = LoadImage("graphics\DeathPitTroll.bmp")
Global panelImg = LoadImage("graphics\DeathPitPanel.bmp")
Global scoresImg = LoadImage("graphics\DeathPitScores.bmp")
	;check images loaded
	If loadingImg = 0 Or titleImg = 0 Or infoImg = 0 Or deathImg = 0 Or winImg = 0 Or heroImg = 0 Or coinImg = 0 Or trollImg = 0 Or panelImg = 0
		error1$ = "An image did not load"
		error2$ = "Please ensure you have not moved/modfied Death Pit's 'graphics' directory"
		Error(error1,error2)
		End
	EndIf

;MAIN *****************************************************

;Seed Rnd
SeedRnd MilliSecs()
LoadHighScores()
DisplayStartUpScreen()

	;MAIN GAME LOOP
	While Not KeyHit(1)
		
		If Not ChannelPlaying(bgChannel)
			bgChannel=PlaySound(themeSound)
		EndIf
		
		;select state
		Select status

			Case 0
				DisplayTitleScreen()

			Case 1
				Cls
				Color 255,255,0
				Rect 0, PANEL_HEIGHT, WIDTH , HEIGHT - PANEL_HEIGHT, 0
				UpdatePlayer()
				UpdateTrollsAndCoins()
				UpdateTime()
				Render()
				Flip
		End Select
	Wend

StopChannel(bgChannel)
DisplayExitScreen()

WaitKey
;Free up memory used for storing font
FreeFont arialFnt
DeleteTrollsAndCoins()
Delete player

End
;**********************************************************

;Load high scores from file
Function LoadHighScores()

	If Not FileType(HSCORE_FILE) = 1
		;open/create a file to write to
		hScores = WriteFile(HSCORE_FILE)
		
		;if unable to open file, file high scores and return
		If hScores = 0
			For i=1 To NUM_OF_HSCORES
				h.highscore = New highscore
				h\score = 1000
				h\name = "FE"
			Next
			Return
		EndIf

		For i=1 To NUM_OF_HSCORES
			WriteInt hScores, 1000
			WriteString hScores, "FE"
		Next
		CloseFile hScores
	EndIf

	hScores = ReadFile(HSCORE_FILE)
	For i=1 To NUM_OF_HSCORES
		h.highscore = New highscore
		h\score = ReadInt(hScores)
		h\name = ReadString(hScores)
	Next
	CloseFile(hScores)
	
End Function

;Initialise Game
Function InitGame()

	;set status to show playing game
	status = 1
	
	;must be called in this order due to a check performed in setCoins()
	setTrolls()
	setCoins()
	
	StartHooter()
	countdown = MAX_TIME
	curTime = MilliSecs()
			
End Function

;Initialise Player
Function InitPlayer()

	player\x = WIDTH / 2
	player\y = HEIGHT / 2
	player\livesLeft = 2
	player\currentLevel = 1
	player\score = 0
	player\speed = START_SPEED
	player\dir = 4

End Function

;Set the troll coordinates. This function must be called before setCoins()
Function SetTrolls()

	;To aid readibility a variable called 'tSize' is created equal to SIZE_OF_TROLLS.
	tSize = SIZE_OF_TROLLS

	For i = 1 To NUM_OF_TROLLS
	
		;checks that trolls do not overlap each other, the player starting point
		;or go out of the playing area
		overlapChk = True
		While overlapChk
			overlapChk = False
			
			tx = Rnd( 2, (WIDTH - 2) - tSize)
			ty = Rnd( (PANEL_HEIGHT + 2), (HEIGHT - 2) - tSize)
			
			For t.troll = Each troll
				If RectsOverlap(t\x,t\y,tSize,tSize,tx,ty,tSize,tSize) Then overlapChk = True
			Next
			
			If RectsOverlap(player\x,player\y,SIZE_OF_PLAYER,SIZE_OF_PLAYER,tx,ty,tSize,tSize) Then
				overlapChk = True
			EndIf
		Wend
		
		t.troll = New troll
		t\x = tx
		t\y = ty
	Next

End Function

;set the coin coordinates. This function must be called after setTrolls() has been done.
;A check is performed to ensure that coin does not overlap trolls, which would be unfair
;to players.
Function setCoins()

	;To aid readibility 2 variables called 'cSize' & 'tSize' are created equal to
	;SIZE_OF_COINS and SIZE_OF_TROLLS
	cSize = SIZE_OF_COINS
	tSize = SIZE_OF_TROLLS

	For i = 1 To NUM_OF_COINS
	
		;checks that coins do not overlap each other, any trolls or the player
		;starting position
		overlapChk = True
		While overlapChk
			overlapChk = False
			
			cx = Rnd( 2, (WIDTH - 2) - cSize)
			cy = Rnd( (PANEL_HEIGHT + 2), (HEIGHT - 2) - tSize)
			
			For c.coin = Each coin
				If RectsOverlap(c\x,c\y,cSize,cSize,cx,cy,cSize,cSize) Then overlapChk = True
			Next
			
			For t.troll = Each troll
				If RectsOverlap(cx,cy,cSize,cSize,t\x,t\y,tSize,tSize) Then overlapChk = True
			Next
			
			If RectsOverlap(player\x,player\y,SIZE_OF_PLAYER,SIZE_OF_PLAYER,cx,cy,cSize,cSize) Then
				overlapChk = True
			EndIf

		Wend
		
		c.coin = New coin
		c\x = cx
		c\y = cy
	Next

End Function

;Render player, trolls and coins on screen
Function Render()

	;prevents images being drawn after player death/win
	If status = 1

		Color 255,255,255

		;draw trolls
		For t.troll = Each troll
			DrawImage(trollImg, t\x, t\y)
		Next
	
		;draw coins
		For c.coin = Each coin
			DrawImage(coinImg, c\x, c\y)
		Next

		;draw player
		DrawImage(heroImg, player\x, player\y)
	
		;display player stats and info
		Color 255, 255, 255
		Text 10, 10, "LEVEL = " + player\currentLevel
		Text 100, 10, "SCORE = " + player\score
		DrawImage(panelImg, (WIDTH/2) - 75, 5)
		Text (WIDTH/2) + 75 + 40, 10, "LIVES = " + player\livesLeft
		If (countdown / 1000) < 11 And ((countdown / 1000) Mod 2) = 0 Then Color 255, 0, 0
		Text (WIDTH - 120), 10, "TIME LEFT = " + (countdown / 1000)
	
		;display message at start of each level
		If player\dir = 4 Then
			Color 255,255,255
			Text (WIDTH/2)-30,(HEIGHT/2)-30,"Ready, GO!"
		EndIf
		
	End If

End Function

;Update Player Position
Function UpdatePlayer()

	;steer player
	If KeyDown(200) Or KeyDown(72) Then player\dir = 0
	If KeyDown(205) Or KeyDown(77) Then player\dir = 1
	If KeyDown(208) Or KeyDown(80) Then player\dir = 2
	If KeyDown(203) Or KeyDown(75) Then player\dir = 3

	;move player
	Select player\dir
	
		Case 0
			player\y = player\y - player\speed
		Case 1
			player\x = player\x + player\speed
		Case 2
			player\y = player\y + player\speed
		Case 3
			player\x = player\x - player\speed
	
	End Select
	
End Function

;Update trolls and coins
Function UpdateTrollsAndCoins()

	;check if coin has been picked and if player has won
	numOfCoinsLeft = 0
	For c.coin = Each coin
		
		numOfCoinsLeft  = numOfCoinsLeft + 1
		If RectsOverlap(player\x,player\y,SIZE_OF_PLAYER,SIZE_OF_PLAYER,c\x,c\y,SIZE_OF_COINS,SIZE_OF_COINS)
			fgChannel=PlaySound(beepSound)
			
			If player\currentLevel = 1 Then player\speed = player\speed + LEVEL1
			If player\currentLevel = 2 Then player\speed = player\speed + LEVEL2
			If player\currentLevel = 3 Then player\speed = player\speed + LEVEL3
			
			Delete c
			player\score = player\score + 100
			numOfCoinsLeft = numOfCoinsLeft - 1
		EndIf
		
	Next
	
	If numOfCoinsLeft = 0
		NextLevel()
		Return
	EndIf
		
	;check if player has collided with kill objects
	For t.troll = Each troll
		
		If RectsOverlap(player\x,player\y,SIZE_OF_PLAYER,SIZE_OF_PLAYER,t\x,t\y,SIZE_OF_TROLLS,SIZE_OF_TROLLS)
			Explode()
			RestartLevel()
		EndIf
	
	Next
	
	;check if player has gone out of bounds
	If player\x < 2 Or player\x+SIZE_OF_PLAYER > WIDTH-2 Or player\y < 42 Or player\y+SIZE_OF_PLAYER > HEIGHT-2 
		Explode()
		RestartLevel()
	End If
		
End Function

;Counts a players remaining time for the level
Function UpdateTime()
	
	If MilliSecs() - curTime > 1000 Then countdown = countdown - 1000 : curTime = MilliSecs()
	
	;if countdown reaches 0 then its end game for the player
	If countdown = 0
		Explode()
		PlayerDead()
	EndIf

End Function

;Restarts the level if player has lives left
Function RestartLevel()

	player\livesLeft = player\livesLeft -1
	If player\livesLeft = 0 Then
		PlayerDead()
	Else
		StartHooter()
		player\x = WIDTH/2
		player\y = HEIGHT/2
		player\dir = 4
	EndIf

End Function

;Used to move the player to the next level, and reinitialise certain variables
Function NextLevel()

	player\currentLevel = player\currentLevel + 1
	player\score = player\score + countdown
	If player\currentLevel = 4 Then 
		PlayerWon()
	Else
		DeleteTrollsAndCoins()
		InitGame()
		player\x = WIDTH/2
		player\y = HEIGHT/2
		player\speed = START_SPEED
		player\dir = 4
	EndIf
	
End Function

;Player has one, show win screen and reset certain variables
Function PlayerWon()

	DeleteTrollsAndCoins()
	StopChannel bgChannel
	If ChannelPlaying(bgchannel)=False bgChannel=PlaySound(winSound)
	DisplayWinScreen()
	StopChannel bgChannel
	status = 0

End Function

;Kill the player, show screen and reset certain variables
Function PlayerDead()

	DeleteTrollsAndCoins()
	StopChannel bgChannel
	fgChannel=PlaySound(deathSound)
	DisplayDeathScreen()
	status = 0

End Function

;Delete trolls And coins, otherwise trolls and uneaten coin will reappear in subsequent goes
Function DeleteTrollsAndCoins()

	Cls
	
	;delete coins
	Delete Each coin
			
	;delete trolls
	Delete Each troll
	
End Function

;Display Startup Screen
Function DisplayStartupScreen()
	
	Cls
	DrawImage(loadingImg,(WIDTH/2) - 240,(HEIGHT/2) - 90)
	Flip
	
	Delay(2 * 1000)
	fgChannel=PlaySound(loadingSound)
	Delay(2 * 1000)

End Function

;Display Title Screen
Function DisplayTitleScreen()
	
	Cls
	DrawImage(titleImg,0,0)
	Flip
	
	If KeyHit(28) Then
		InitPlayer()
		InitGame()
	Else If KeyHit(31)
		DisplayHighScores()
	Else If KeyHit(23)
		DisplayInfo()
	End If

End Function

;Display Win Screen
Function DisplayWinScreen()
	
	name$ = ""
	Color 255,255,0
	
	While Not KeyHit(28)
		Cls
		DrawImage(winImg,0,0)

		If CheckPlayerScore()
			a=GetKey()
			If a >= 32 And a <= 122 And Len(name) < 5
				name = name + Chr$(a)
			EndIf
		
			If (KeyHit(14) Or KeyHit(211)) And Len(name) > 0 Then name = Left$(name, Len(name)-1)

			Text (WIDTH/2) - 150, (HEIGHT/2) + 135, "You have achieved a high score: " + player\score
			Text (WIDTH/2) - 150, (HEIGHT/2) + 150, "Please enter your name: " + name
		Else
			Text (WIDTH/2) - 60, (HEIGHT/2) + 135, "You Scored " + player\score
		EndIf
		
		Flip
	Wend
	UpdateHighScores(name)
	DisplayHighScores()
	
	FlushKeys

End Function

;Display Highscores
Function DisplayHighScores()

	Cls
	DrawImage(scoresImg, 0, 0)
	Color 255, 255, 0
	
	y = 150
	For h.highscore = Each highscore
		Text (WIDTH / 2) - 75, y, h\score
		Text (WIDTH / 2) + 25, y, h\name
		y = y + 15
	Next
	
	Flip
	
	stopTime = MilliSecs() + 10000
	While MilliSecs() < stopTime
		If KeyHit(28) Then Exit
	Wend
	
	FlushKeys

End Function

;Display Information
Function DisplayInfo()

	Cls
	DrawImage(infoImg, 0, 0)
	
	Flip
	
	stopTime = MilliSecs() + 10000
	While MilliSecs() < stopTime
		If KeyHit(28) Then Exit
	Wend
	
	FlushKeys

End Function

;Display Death Screen
Function DisplayDeathScreen()

	name$ = ""
	Color 255,255,0
	
	While Not KeyHit(28)
		Cls
		DrawImage(deathImg,0,0)

		If CheckPlayerScore()
			a=GetKey()
			If a >= 32 And a <= 122 And Len(name) < 5
				name = name + Chr$(a)
			EndIf
		
			If (KeyHit(14) Or KeyHit(211)) And Len(name) > 0 Then name = Left$(name, Len(name)-1)

			Text (WIDTH/2) - 150, (HEIGHT/2) + 20, "You have achieved a high score: " + player\score
			Text (WIDTH/2) - 150, (HEIGHT/2) + 35, "Please enter your name: " + name
		Else
			Text (WIDTH/2) - 60, (HEIGHT/2) + 20,"You Scored " + player\score
		EndIf
		
		Flip
	Wend
	UpdateHighScores(name)
	DisplayHighScores()
	
	FlushKeys

End Function

;Display Exit screen
Function DisplayExitScreen()
	
	Color 221, 0, 0
	x = (WIDTH/2)
	y = (HEIGHT/2) - 100
	
	Cls
	Text x, y, "THANK YOU", True, False
	y = y + FontHeight() + 20
	Text x, y, "Big thanks to everyone who contributed art and code to", True, False
	y = y + FontHeight() + 2
	Text x, y, "Blitz Basic 2D, especially George Bray, for the excellent", True, False
	y = y + FontHeight() + 2
	Text x, y, "tutorial which led to the inspiration for this game.", True, False
	y = y + FontHeight() + 10
	Text x, y, "Also big thanks to Xtreme Games LLC for", True, False
	y = y + FontHeight() + 2
	Text x, y, "the sounds.", True, False
	y = y + FontHeight() + 20
	Text x, y, "Feersum Endjin", True, False
	Flip
	
	Delay(4*1000)

End Function

;Inserts a successful players score in the high score list and saves the list
Function UpdateHighScores(playerName$)

	
	For h.highscore = Each highscore
		If player\score > h\score Then 
			newH.highscore = New highscore
			newH\score = player\score
			newH\name = playerName
			Insert newH Before h
			Delete Last highscore
			Exit
		EndIf
	Next
	
	;open/create a file to write to
	hScores = WriteFile(HSCORE_FILE)
	
	;if unable to open file, return
	If hScores = 0 Then Return
	For h.highscore = Each highscore
		WriteInt hScores, h\score
		WriteString hScores, h\name
	Next

	CloseFile(hScores)

End Function

;This functions tests whether a players score is greater than any current high scores
Function CheckPlayerScore()

	For h.highscore = Each highscore
		If player\score > h\score Then Return True
	Next
	
	Return False

End Function

;The sound to indicate the start of a level
Function StartHooter()

	;play level start sound effect
	fgChannel=PlaySound(lvlStartSound)

End Function

;Explode function - taken & adpated from the Blitz Basic sample program "bombtrail"
Function Explode()

	;Play killed sound
	StopChannel fgChannel
	fgChannel=PlaySound(killedSound)

	;Repeat the following loop until the fade colour values have finally gone from 
	;black To yellow To white To black
	Repeat

		; Increase yellow colour value
		yellow=yellow+10

		; If yellow colour value exceeds maximum (255), then start increasing white colour value
		If yellow>255 Then yellow=255 : white=white+10

		; If white colour value exceeds maximum, then start increasing black colour value
		If white>255 Then black=black+10 : white=255

		; Set clear screen colour
		ClsColor yellow-black,yellow-black,white-black

		; Clear screen
		Cls

		; Flip screen bufffers
		Flip

	Until black>255
	ClsColor 0, 0, 0

End Function

;Error Function
Function Error(error1$,error2$)

	Cls
	Text 20,40,error1$
	Text 20,60,error2$
	Text 20,80,"Press 'enter' to continue"
	Flip
	WaitKey
	
End Function
		