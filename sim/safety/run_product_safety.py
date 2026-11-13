#!/usr/bin/env python3
"""Compile real product command/store/config/egress code against headless Minecraft boundary doubles.
Breaks caught: log-only kick/ban/pardon/unban mutates cases, disconnect escapes, webhook constructs
an HTTP request, inherited config enables unqualified veil. Not a Minecraft launch or mixin test.
"""
import os, pathlib, subprocess, tempfile, glob
ROOT = pathlib.Path(__file__).resolve().parents[2]
JDK = pathlib.Path(os.environ.get('JAVA_HOME', str(pathlib.Path.home()/'jdk25')))/'bin'
CACHE = pathlib.Path(os.environ.get('GRADLE_USER_HOME', str(pathlib.Path.home()/'.gradle-meshac')))
def dependency(pattern):
    files=list(CACHE.rglob(pattern)); assert files, pattern
    return str(files[-1])
cp=os.pathsep.join(dependency(x) for x in ['brigadier-*.jar','gson-2.14.0.jar','slf4j-api-2.0.7.jar'])
stubs={
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat; public record Component(String text) { public static Component literal(String s) {return new Component(s);} public String getString(){return text;} }''',
'net/minecraft/network/protocol/Packet.java': '''package net.minecraft.network.protocol; public interface Packet<T> {}''',
'net/minecraft/server/level/ServerPlayer.java': '''package net.minecraft.server.level;
import java.util.*; import net.minecraft.network.chat.Component;
public class ServerPlayer { public final Connection connection=new Connection(); public final UUID id=UUID.randomUUID(); public final Profile profile=new Profile("fixture");
public record Profile(String name) {} public UUID getUUID(){return id;} public Profile getGameProfile(){return profile;} public Component getName(){return Component.literal(profile.name());} public void sendSystemMessage(Component c){} public net.minecraft.commands.CommandSourceStack.Server level(){return new net.minecraft.commands.CommandSourceStack().new Server();} public void stopRiding(){} public static class Connection {public int disconnects; public void disconnect(Component c){disconnects++;} public void teleport(double x,double y,double z,float a,float b){} public void send(net.minecraft.network.protocol.Packet<?> p){}} }''',
'net/minecraft/commands/CommandSourceStack.java': '''package net.minecraft.commands; import java.util.*; import java.util.function.*; import net.minecraft.network.chat.Component; import net.minecraft.server.level.ServerPlayer;
public class CommandSourceStack { public final ServerPlayer player=new ServerPlayer(); public final List<String> messages=new ArrayList<>(); public ServerPlayer getPlayer(){return player;} public String getTextName(){return "fixture-staff";} public void sendSuccess(Supplier<Component>s,boolean b){messages.add(s.get().getString());} public void sendFailure(Component c){messages.add(c.getString());} public Server getServer(){return new Server();} public class Server {public Server getServer(){return this;} public Players getPlayerList(){return new Players();}} public class Players {public ServerPlayer getPlayer(java.util.UUID id){return player;} public ServerPlayer getPlayerByName(String n){return n.equals("fixture")?player:null;}} }''',
'net/minecraft/commands/Commands.java': '''package net.minecraft.commands; import com.mojang.brigadier.builder.*; import com.mojang.brigadier.arguments.*; public class Commands {public static LiteralArgumentBuilder<CommandSourceStack> literal(String s){return LiteralArgumentBuilder.literal(s);} public static <T> RequiredArgumentBuilder<CommandSourceStack,T> argument(String s,ArgumentType<T>a){return RequiredArgumentBuilder.argument(s,a);} public static final int LEVEL_ADMINS=4; public static java.util.function.Predicate<CommandSourceStack> hasPermission(int i){return s->true;}}''',
'net/minecraft/commands/arguments/EntityArgument.java': '''package net.minecraft.commands.arguments; import com.mojang.brigadier.arguments.*; import com.mojang.brigadier.context.*; import net.minecraft.commands.*; import net.minecraft.server.level.*; public class EntityArgument {public static StringArgumentType player(){return StringArgumentType.word();} public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> c,String s){return c.getSource().player;}}''',
'net/fabricmc/loader/api/FabricLoader.java': '''package net.fabricmc.loader.api; public class FabricLoader {public static final FabricLoader INSTANCE=new FabricLoader(); public static FabricLoader getInstance(){return INSTANCE;} public java.nio.file.Path getConfigDir(){return java.nio.file.Path.of(System.getProperty("fixture.config"));}}''',
'dev/meshac/product/Screens.java': '''package dev.meshac.product; import net.minecraft.network.chat.Component; public class Screens {public static Component kick(String s,Cases.Case c){return Component.literal(s);} public static Component ban(Cases.Case c){return Component.literal(c.reason);}}''',
'dev/meshac/veil/Veil.java': '''package dev.meshac.veil; public class Veil {public static boolean xrayOn,espOn,cullMobs; public static void clear(){}}'''
}
with tempfile.TemporaryDirectory(prefix='meshac-product-test-') as tmp:
    t=pathlib.Path(tmp); files=[]
    for name,text in stubs.items():
        p=t/name; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text); files.append(str(p))
    for name in ['product/MeshCommands','product/Cases','product/Config','product/Ladder','product/Discord','product/Perm','product/Verdict','safety/Effects','safety/Containment']:
        files.append(str(ROOT/'src/main/java/dev/meshac'/ (name+'.java')))
    files.append(str(ROOT/'sim/safety/ProductSafetyTest.java'))
    subprocess.run([str(JDK/'javac'),'-cp',cp,'-d',str(t/'classes'),*files],check=True)
    subprocess.run([str(JDK/'java'),'-cp',str(t/'classes')+os.pathsep+cp,'-Dfixture.config='+str(t),'ProductSafetyTest'],check=True)
