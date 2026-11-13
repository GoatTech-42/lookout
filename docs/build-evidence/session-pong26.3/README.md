# Changed-artifact 26.3 normal join smoke

Production sourced7ffae827ef77593e50d78e4aef573b3ece7e6d9. Shipping and installed jar SHA2562f7b63892b17c87b5de0111c32e0263b145df6cc95d5f8878ab8f2e9888f0ec8. The same separate read-only observer from anchor26.3 was reused without shipping patches. See that directory for observer source and original setup. Current manifest/logs are specific to this artifact.

The disposable offline client and Fabric server ran together in a fresh user/network namespace, loopback only and no external route, endpoint127.0.0.1:25587, test identity MeshRigA. No real account. All179 checks gated, LOG_ONLY and Veil false/false/false. Case bytes unchanged before/after SHA25637c53c19d9eb419e442a41d2c731673ff7765805729002c494d89033c082bc96. Runner and enclosing execution both exit0, no lingering Minecraft/server/Xvfb processes after shutdown.

Native login PRE/POST write at12:57:37 uses entity50; initial anchor-11023 follows at12:57:37 with handshake false and matching live identities. At12:57:38 matching PONG passes the changed common hook and handshake becomes true, pending0, native/stored player IDs50, listener/connection/player identities true. ServerVersionV_26_3 observed. All16 shipping mixins applied, including the changed common listener. The positive configuration-phase ping is unrelated to the anchor.

Screenshot inspected directly: rendered flat world, hand, hearts/hunger/hotbar and movement tutorial visible, not Loading terrain. This is only normal join visual evidence. Offline service-discovery/authentication/hostname and unavailable audio-device warnings remain in logs and are not detector failures.

This smoke proves the new admission predicates accept the observed normal native initial PONG. It does NOT establish native exclusion or reachability of synthetic stale callbacks; that defensive contract remains actual-class fixture/mutation evidence. No detector qualification, global ownership, postwrite ordering, same-session reconfiguration, broader lifecycle or other-version runtime qualification follows. No duplicate-anchor policy changed. No enforcement/gate/Veil opened.
