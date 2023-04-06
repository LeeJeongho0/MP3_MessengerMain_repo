package com.example.outtermodulebindservicemessenger

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.outtermodulebindservicemessenger.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    lateinit var binding:ActivityMainBinding
    lateinit var receiveMessenger: Messenger
    lateinit var sendMessenger: Messenger
    lateinit var messengerConnection: ServiceConnection
    // 코루틴 실행 객체
    var progressCoroutineScopeJob : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1.2 receiveMessenger 생성 (10이 돌아오면 프로그래스바를 진행하고, 서비스에 20을 보내고, 서비스를 취소하고, 코루틴을 종료)
        receiveMessenger = Messenger(HandlerReplyMsg())

        // 2. 서비스 커넥션을 생성
        messengerConnection = object: ServiceConnection{
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                sendMessenger = Messenger(service)
                // 10번 명령을 전달. receiveMessenger 전달
                val message = Message()
                message.replyTo = receiveMessenger
                message.what = 10
                sendMessenger.send(message)
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                Toast.makeText(applicationContext,"서비스 종료",Toast.LENGTH_SHORT).show()
            }

        }

        // 3. play 버튼을 누르면 bindService 실행
        binding.messengerPlay.setOnClickListener {
            val intent = Intent("ACTION_SERVICE_MESSENGER")
            intent.setPackage("com.example.mp3servicemessenger")
            bindService(intent,messengerConnection, Context.BIND_AUTO_CREATE)
        }

        // 4. 정지버튼을 누르면 20을 서비스에 전송해서 노래를 정지하고, bindService를 종료하고, 코루틴 종료
        binding.messengerStop.setOnClickListener {
            val message = Message()
            message.what = 20
            sendMessenger.send(message)
            unbindService(messengerConnection)
            progressCoroutineScopeJob?.cancel()
            binding.messengerProgress.progress = 0
        }



    }

    // 1.1 서비스에서 전송해올 메세지를 받기위한 핸들러
    inner class HandlerReplyMsg: Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                // 프로그래스바를 코루틴으로 실행
                10 -> {
                    val bundle = msg.obj as Bundle
                    val duration = bundle.getInt("duration")
                    if(duration > 0){
                        binding.messengerProgress.max = duration
                        val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
                        progressCoroutineScopeJob = backgroundScope.launch {
                            while(binding.messengerProgress.progress < binding.messengerProgress.max){
                                delay(1000)
                                binding.messengerProgress.incrementProgressBy(1000)
                            }//end of while
                            binding.messengerProgress.progress = 0

                            // 종료 메세지를 서비스에 전달해야함
                            val message = Message()
                            message.what = 20
                            sendMessenger.send(message)

                            unbindService(messengerConnection)
                            progressCoroutineScopeJob?.cancel()
                        }
                    }
                }//end of 10
                20 -> {}
            }//end of when
        }//end of handlemessage
    }//end of inner class
}