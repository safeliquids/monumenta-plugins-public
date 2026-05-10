package com.playmonumenta.plugins.bosses.bosses.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.aurora.*;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.protocollib.CursedListener;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.bosses.spells.aurora.SpellMoonlightSlash.COOLDOWN_REDUCTION;


public class Aurora extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_aurora";
	public static final int DETECTION_RANGE = 100;
	public static final int PROTECTED_RADIUS = 2;
	public static final int ARENA_RADIUS = 29;
	public static final int ARENA_SEMI_OUTER_RADIUS = 22;
	public static final int ARENA_INNER_RADIUS = 12;
	public static final int SPELL_INTERVAL = 6 * 20;
	public static final TextColor AURORA_COLOR = TextColor.color(0xa175ff);
	public static final TextColor DELFIA_COLOR = TextColor.color(0xeb9bad);

	private static final double BASE_MAX_HEALTH = 20000;
	private static final int GROWABLE_MAX_ID = 3;
	private static final String CHARGES_SCORE = "AuroraLoomCharges";
	// Handled in mechs
	private static final String ALIVE_TAG = "AuroraFighter";
	private static final String AURORA_LOOM_NAME = "Loamskattar Catalyst";

	// NOT spawnLoc, it is the block directly below that!!!
	private final Location mCenterBlock;
	private final Set<Player> mCdrWarned = new HashSet<>();
	private final List<Spell> mPassivesMinis;
	private final List<Block> mBrokenBlocks;
	private final List<BukkitTask> mActiveTasks = new ArrayList<>();
	private final SpellAuroraAdvancements mAdvancements;
	private final SpellCelestialPillars mCelestialPillars;
	private final SpellStarlightSurge mStarlightSurge;
	private final SpellFocusedMoonbeams mMoonbeams;
	private final SpellAuroraMinis mAuroraMinis;
	private final SpellSupernova mSupernova;
	private final SpellStarShower mStarShower;
	private final SpellPulsarRadiation mPulsarRadiation;
	private final SpellCollapsingConnection mCollapsingConnection;
	private final int mRage;

	private boolean mLastPhase = false;

	@FunctionalInterface
	public interface BlockRepairer {
		void repair(Collection<Block> blocks);
	}

	@FunctionalInterface
	public interface BlockDestroyer {
		void destroy(Collection<Block> blocks);
	}

	public record Dialogue(String message, String speaker, TextColor textColor) {
		public static Dialogue aurora(String message) {
			return new Dialogue(message, "[Aurora]", AURORA_COLOR);
		}

		public static Dialogue delfia(String message) {
			return new Dialogue(message, "[Delfia]", DELFIA_COLOR);
		}

		public static Dialogue of(String message, String speaker, TextColor textColor) {
			return new Dialogue(message, speaker, textColor);
		}
	}

	public Aurora(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		spawnLoc.setY(spawnLoc.getBlockY());
		mCenterBlock = spawnLoc.clone().subtract(0, 1, 0);
		mBrokenBlocks = new ArrayList<>(250);

		List<Player> players = playersInRange(spawnLoc, true);
		mRage = players.stream()
			.map(player -> ScoreboardUtils.getScoreboardValue(player, "AuroraRage").orElse(0))
			.max(Comparator.comparingInt(Integer::intValue))
			.orElse(0);

		players.forEach(player -> {
			boolean foundWorldshaper = false;
			for (ItemStack itemStack : player.getInventory()) {
				if (isAuroraLoom(itemStack)) {
					foundWorldshaper = true;
					break;
				}
			}
			if (!foundWorldshaper && !isAuroraLoom(player.getItemOnCursor())) {
				InventoryUtils.giveItemFromLootTable(player, SpellCelestialPillars.GIVEN_ITEM_KEY, 1);
			}
		});

		List<Spell> phase1Actives = new ArrayList<>();
		List<Spell> phase2Actives = new ArrayList<>();
		List<Spell> phase3Actives = new ArrayList<>();
		List<Spell> phase4Actives = new ArrayList<>();

		mAdvancements = new SpellAuroraAdvancements(spawnLoc, players);
		mCelestialPillars = new SpellCelestialPillars(plugin, spawnLoc, mRage, players.size(), Aurora::onPickupPillarMatter, this::repairBlocks);
		mStarShower = new SpellStarShower(plugin, boss, spawnLoc, mCelestialPillars, mAdvancements::onStarShowerBreak, this::destroyBlocks);
		mStarlightSurge = new SpellStarlightSurge(plugin, boss, spawnLoc, mRage >= 140 ? 2 : 1, mCelestialPillars, mAdvancements::onSurgeDebuff, mRage >= 200 ? 20 : 12);
		mMoonbeams = new SpellFocusedMoonbeams(plugin, boss, spawnLoc, mRage >= 80 ? 3 : 2, this::destroyBlocksGenerous);
		mPulsarRadiation = new SpellPulsarRadiation(plugin, boss, mRage >= 140 ? 1.5 : 1, spawnLoc);
		mSupernova = new SpellSupernova(plugin, boss, spawnLoc);
		mAuroraMinis = new SpellAuroraMinis(plugin, boss, spawnLoc, mRage, mRage >= 80, this::destroyBlocks);
		mCollapsingConnection = new SpellCollapsingConnection(plugin, boss, spawnLoc, mRage >= 200 ? (mRage / 10 - 15) : 3, this::destroyBlocks);

		SpellMeteorRain rageStarShower = new SpellMeteorRain(plugin, boss, spawnLoc);
		SpellScorchingStar spellScorchingStar = new SpellScorchingStar(plugin, boss, mRage >= 200 ? 0.003 : 0);
		SpellStardustBlaster stardustBlaster = new SpellStardustBlaster(plugin, boss, spawnLoc, 8, mRage >= 80, mRage >= 200 ? 3 : 2);
		SpellQuasarStep quasarStep = new SpellQuasarStep(plugin, boss, spawnLoc);
		SpellGalacticThunderstorm galacticThunderstorm = new SpellGalacticThunderstorm(plugin, boss, spawnLoc);
		// rage spells
		SpellSpatialShattering spatialShattering = new SpellSpatialShattering(plugin, boss, spawnLoc);
		SpellAstralGreatsword astralGreatsword = new SpellAstralGreatsword(plugin, boss, spawnLoc, mRage, this::destroyBlocks);

		phase1Actives.add(mStarShower);
		phase1Actives.add(quasarStep);
		phase1Actives.add(spellScorchingStar);
		phase1Actives.add(stardustBlaster);

		phase2Actives.add(mStarShower);
		phase2Actives.add(quasarStep);
		phase2Actives.add(new SpellFrigidCollapse(plugin, boss));
		phase2Actives.add(stardustBlaster);

		phase3Actives.add(mStarShower);
		phase3Actives.add(quasarStep);
		phase3Actives.add(stardustBlaster);

		phase4Actives.add(stardustBlaster);
		phase4Actives.add(spellScorchingStar);
		phase4Actives.add(quasarStep);
		phase4Actives.add(quasarStep);
		phase4Actives.add(new SpellAstralCollapse(plugin, boss, spawnLoc, blocks -> doBlockBreak(blocks, 0, 0), mAdvancements::blackHoleEatMob));
		phase4Actives.add(galacticThunderstorm);

		if (mRage >= 40) {
			phase1Actives.add(spatialShattering);
			phase2Actives.add(spatialShattering);
			phase3Actives.add(spatialShattering);
		}
		if (mRage >= 60) {
			phase2Actives.add(astralGreatsword);
			phase3Actives.add(astralGreatsword);
		}
		if (mRage >= 100) {
			phase2Actives.add(new SpellStardustDetonation(plugin, boss, spawnLoc, 7, mRage >= 200 ? this::destroyBlocksHarsh : this::destroyBlocks));
			phase3Actives.add(new SpellStardustDetonation(plugin, boss, spawnLoc, 7, mRage >= 200 ? this::destroyBlocksHarsh : this::destroyBlocks));
			phase4Actives.add(new SpellStardustDetonation(plugin, boss, spawnLoc, mRage >= 200 ? 5 : 4, this::destroyBlocks));
		}
		if (mRage >= 160) {
			phase1Actives.add(rageStarShower);
			phase2Actives.add(rageStarShower);
			phase3Actives.add(rageStarShower);
			phase4Actives.add(rageStarShower);
		}
		if (mRage >= 200) {
			phase4Actives.add(galacticThunderstorm);
		}

		SpellAuroraBlockPlacer blockPlacer = new SpellAuroraBlockPlacer(boss, spawnLoc, mCelestialPillars);
		SpellMoonlightSlash moonlightSlash = new SpellMoonlightSlash(plugin, boss, mRage >= 80 ? 0.8 : 1.0, false, this::moonlightCdr);
		SpellAuroraVoid voidSpell = new SpellAuroraVoid(spawnLoc, mAdvancements::onVoid);
		SpellShieldStun shieldStun = new SpellShieldStun(10 * 20);
		SpellBaseParticleAura blightParticleAura = new SpellBaseParticleAura(boss, 5, b -> {
			new PartialParticle(Particle.SCULK_SOUL, b.getLocation().add(0, 1, 0))
				.count(20)
				.delta(0.25, 0.4, 0.25)
				.extra(0.05)
				.spawnAsBoss();
		});


		List<Spell> mBasePassives = List.of(
			mAdvancements,
			blockPlacer,
			new AuroraConditionalTp(boss, spawnLoc, location -> fancyTp(location, 15)),
			voidSpell,
			new SpellAuroraMobs(plugin, boss, spawnLoc, 3.0 / (2 + players.size()), mRage),
			shieldStun
		);

		List<Spell> phase1Passives = new ArrayList<>(mBasePassives);
		List<Spell> phase2Passives = new ArrayList<>(mBasePassives);
		List<Spell> phase3Passives = new ArrayList<>(mBasePassives);
		List<Spell> phase4Passives = new ArrayList<>(mBasePassives);

		phase1Passives.add(moonlightSlash);
		phase2Passives.add(moonlightSlash);
		phase3Passives.add(moonlightSlash);
		phase4Passives.add(new SpellMoonlightSlash(plugin, boss, mRage >= 80 ? 0.8 : 1.0, true, this::moonlightCdr));

		phase3Passives.add(blightParticleAura);
		phase4Passives.add(blightParticleAura);

		mPassivesMinis = List.of(mAdvancements, blockPlacer, voidSpell, shieldStun);

		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();

		events.put(90, e -> {
			showerAndSurge();
		});

		events.put(80, e -> {
			showerAndSurge();
		});

		events.put(75, e -> {
			knockbackPlayers();

			changePhase(SpellManager.EMPTY, mPassivesMinis, null);
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);
			mCollapsingConnection.onStartAbsorbPower();

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				EntityUtils.selfRoot(mBoss, 6 * 20);
				absorbPower(() -> {
					changePhase(new SpellManager(phase2Actives), phase2Passives, null);
					mCelestialPillars.run();
					mCollapsingConnection.onStopAbsorbPower();
					if (mRage >= 60) {
						forceCastSpell(SpellAstralGreatsword.class);
					}
				});
			}, 20);
		});

		events.put(70, e -> {
			showerAndSurge();
		});

		events.put(60, e -> {
			showerAndSurge();
		});

		events.put(50, e -> {
			knockbackPlayers();
			mCollapsingConnection.cancel();

			changePhase(SpellManager.EMPTY, mPassivesMinis, null);
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);

			Location raisedCenter = spawnLoc.clone().add(0, 45, 0);


			EntityUtils.selfRoot(mBoss, 10 * 20);
			Bukkit.getScheduler().runTaskLater(plugin, () -> absorbPower(() -> {
				int dialogueDelay = 20;
				final int animationTime = 2 * 20;
				@Nullable
				BossBarManager auroraBossBar = getBossBar();
				if (auroraBossBar == null) {
					MMLog.severe("Aurora has null BossBarManager!");
					return;
				}

				auroraBossBar.setVisible(false);
				mBoss.setInvulnerable(true);
				mBoss.setGravity(false);
				mBoss.setVelocity(new Vector());

				mActiveTasks.add(new BukkitRunnable() {
					private final World mWorld = mBoss.getWorld();
					private int mGrowableSuffix = 1;
					int mTicks = 0;

					@Override
					public void run() {
						if (mTicks == 20) {
							mBoss.setVelocity(new Vector(0, 1.9, 0));
							mBoss.setRotation(mBoss.getYaw(), 90);
						}
						if (mTicks >= animationTime) {
							mBoss.teleport(spawnLoc.clone().add(0, 5, 0));
							mBoss.setVelocity(new Vector());
							mBoss.setAI(false);

							mWorld.playSound(boss.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 5.0f, 1.25f);

							mBoss.addScoreboardTag("boss_player[skinname=aurora2]");
							players.forEach(player -> player.hideEntity(mPlugin, mBoss));
							CursedListener.updateFakePlayer(mBoss);

							this.cancel();
							return;
						}

						float progress = (float) mTicks / animationTime;
						new PPPillar(Particle.REDSTONE, mSpawnLoc, 44)
							.count(150)
							.delta(progress, 0, progress)
							.data(new Particle.DustOptions(ParticleUtils.getTransition(Color.fromRGB(162, 0, 211), Color.fromRGB(208, 96, 213), progress), 1.1f + progress / 2))
							.spawnAsBoss();

						if (mGrowableSuffix <= GROWABLE_MAX_ID && mTicks % 10 == 0) {
							GrowableAPI.grow("AuroraVeilTear" + mGrowableSuffix, raisedCenter, 1, 20, false);
							mGrowableSuffix++;

							mWorld.playSound(boss.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.HOSTILE, 5.0f, 2 * progress);
							mWorld.playSound(boss.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.HOSTILE, 5.0f, 2 * progress);
						}
						mTicks++;
					}
				}.runTaskTimer(mPlugin, 0, 1));

				dialogue(dialogueDelay, List.of(
					Dialogue.aurora("No! I can regain control! Delfia! Empty One! I am one of you!"),
					Dialogue.aurora("The Stars gave me a gift. I am chosen. I AM IN CONTROL!"),
					Dialogue.of("ɴᴏ. ᴡᴇ ᴀʀᴇ ᴏɴᴇ, ᴛᴏɢᴇᴛʜᴇʀ. ᴄʜᴀɴɢᴇᴅ. ᴀꜱᴄᴇɴᴅᴇᴅ. ꜱɪꜱᴛᴇʀꜱ! ʜᴇᴀʀ ᴏᴜʀ ᴄᴀʟʟ!", "[Aurora?]", AURORA_COLOR),
					Dialogue.of("ᴡᴇ ʜᴀᴠᴇ ɴᴇᴇᴅ ᴏꜰ ʏᴏᴜʀ ꜱᴛʀᴇɴɢᴛʜ. ᴏꜰꜰᴇʀ ɪᴛ ᴜɴᴛᴏ ᴜꜱ!", "[Aurora] [Delfia]", DELFIA_COLOR),
					Dialogue.of("₣Ɇ₳₴₮, ĐɆ₳Ɽ ₴ł₴₮ɆⱤ. ₩Ɇ Ø₱Ɇ₦ ØɄⱤ ₵ØⱤɆ₴ ₮Ø ɎØɄ. ØɄⱤ ฿Ⱡł₲Ⱨ₮฿ⱠØØĐ ₩łⱠⱠ ₲Ɽ₳₦₮ ɎØɄ ₴Ʉ₴₮Ɇ₦₳₦₵Ɇ. ł₮ ₴Ⱨ₳ⱠⱠ Ɽ₳ł₦ Ʉ₦₮Ø ɎØɄ, ₮ⱧɆ₦ ₩Ɇ ₴Ⱨ₳ⱠⱠ ₳₩₳ł₮ ɎØɄⱤ ₵Ø₥ł₦₲.", "[Hypollote]", TextColor.color(0xff75a1))
				), () -> {
					// start miniboss only after dialogue from the 2 remaining sisters
					mAuroraMinis.summonMinibosses(raisedCenter, () -> {
						mBoss.setAI(true);
						auroraBossBar.setVisible(true);
						voidSpell.setSupernova(true);
						changePhase(SpellManager.EMPTY, mPassivesMinis, null);

						dialogue(30, List.of(
							Dialogue.delfia("ɪ ᴀᴍ ʀᴇᴀᴅʏ ᴛᴏ ʀᴀᴠᴀɢᴇ ᴛʜɪꜱ ᴡᴏʀʟᴅ."),
							Dialogue.delfia("ʏᴏᴜ, ᴡᴏʀᴍ. ꜱᴛᴏᴘ ʏᴏᴜʀ ᴡʀɪɢɢʟɪɴɢ.")
						));
						clearVeilTear(raisedCenter);

						mSupernova.run(() -> {
							mBoss.setGravity(true);
							Location skyLoc = boss.getLocation();
							int delay = 2 * 20;

							new PPLine(Particle.SOUL_FIRE_FLAME, skyLoc, spawnLoc)
								.countPerMeter(6)
								.delta(0.1)
								.extraRange(0, 0.1)
								.delay(delay)
								.spawnAsBoss();

							new PPLine(Particle.END_ROD, skyLoc, spawnLoc)
								.countPerMeter(6)
								.delta(0.5)
								.delay(delay)
								.spawnAsBoss();

							mActiveTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
								changePhase(new SpellManager(phase3Actives), phase3Passives, null, 3 * 20);
								mPulsarRadiation.run();
								mCelestialPillars.run();
								voidSpell.setSupernova(false);
							}, delay));
						});
						// show players after tp to not show the tp animation
						players.forEach(player -> player.showEntity(mPlugin, mBoss));
					});
				}, dialogueDelay);
			}), 20);
		});

		events.put(40, e -> {
			mAdvancements.enableCoordinationCheck();
			showerAndSurge();
		});

		events.put(30, e -> {
			showerAndSurge();
		});

		events.put(25, e -> {
			knockbackPlayers();
			changePhase(SpellManager.EMPTY, mPassivesMinis, null);

			mLastPhase = true;
			mBoss.setAI(false);
			mBoss.setInvulnerable(true);

			dialogue(30, List.of(
				Dialogue.delfia("ᴛʜᴇ ꜰᴀʙʀɪᴄ ᴏꜰ ᴛʜɪꜱ ᴡᴏʀʟᴅ ɪꜱ ꜱᴏ ᴍᴜᴄʜ ᴇᴀꜱɪᴇʀ ᴛᴏ ᴅᴇꜱᴛʀᴏʏ ꜰʀᴏᴍ ᴛʜɪꜱ ꜱɪᴅᴇ. ᴏᴘᴇɴ. ᴍʏ ꜱɪꜱᴛᴇʀꜱ, ɪ ᴀᴍ ʀᴇᴛᴜʀɴɪɴɢ. ᴡᴇ ꜱʜᴀʟʟ ꜰᴇᴀꜱᴛ ᴀᴛ ʟᴀꜱᴛ."),
				Dialogue.aurora("No! I! Am! Still! Here!"),
				Dialogue.delfia("ʏᴏᴜ ᴡɪʟʟ ɴᴏᴛ ʀᴇᴍᴀɪɴ.")
			), () -> {
				Location skyLoc = spawnLoc.clone().add(0, 24, 0);
				skyLoc.setPitch(90);
				fancyTp(skyLoc, 5, () -> {
					mBoss.setAI(false);
					spawnBlackHole(skyLoc, phase4Actives, phase4Passives);
				});
			}, 0);
		});

		startBoss(players, phase1Actives, phase1Passives, new BossBarManager(mBoss, DETECTION_RANGE, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_12, events, false, true));
	}

	private void clearVeilTear(Location raisedCenter) {
		new BukkitRunnable() {
			private final World mWorld = mBoss.getWorld();
			private int mGrowableSuffix = GROWABLE_MAX_ID;

			@Override
			public void run() {
				for (int x = -12; x <= 12; x++) {
					for (int z = -12; z <= 12; z++) {
						raisedCenter.clone().add(x, 0, z).getBlock().setType(Material.AIR);
					}
				}
				if (mGrowableSuffix <= 0) {
					this.cancel();
					return;
				}
				GrowableAPI.grow("AuroraVeilTear" + mGrowableSuffix, raisedCenter, 1, 100, false);

				mWorld.playSound(raisedCenter, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.HOSTILE, 5.0f, 2.0f * mGrowableSuffix / GROWABLE_MAX_ID);
				mGrowableSuffix--;
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	private void spawnBlackHole(Location skyLoc, List<Spell> actives, List<Spell> passives) {
		int spiralDuration = 50;

		new PPSpiral(Particle.SONIC_BOOM, skyLoc, ARENA_RADIUS)
			.countPerBlockPerCurve(5)
			.ticks(spiralDuration)
			.curves(4)
			.curveAngle(720)
			.reversed(true)
			.spawnAsBoss();

		new PPSpiral(Particle.END_ROD, skyLoc, ARENA_RADIUS)
			.countPerBlockPerCurve(10)
			.ticks(spiralDuration)
			.curves(4)
			.curveAngle(-720)
			.reversed(true)
			.spawnAsBoss();

		mActiveTasks.add(new BukkitRunnable() {
			private final World mWorld = mBoss.getWorld();
			int mTicks = 0;

			@Override
			public void run() {
				mWorld.playSound(skyLoc, Sound.UI_TOAST_OUT, 10.0f, 2.0f * mTicks / spiralDuration);
				mWorld.playSound(skyLoc, Sound.UI_TOAST_OUT, 10.0f, 2.0f * mTicks / spiralDuration);
				mWorld.playSound(skyLoc, Sound.UI_TOAST_IN, 10.0f, 2.0f - 2.0f * mTicks / spiralDuration);

				mTicks++;
				if (mTicks >= spiralDuration) {
					Location starLoc = mSpawnLoc.clone().add(0, 2.5, 0);

					mWorld.playSound(skyLoc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 5.0f, 1.3f);
					mWorld.playSound(skyLoc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 5.0f, 0.5f);
					mWorld.playSound(skyLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 5.0f, 1.4f);
					mWorld.playSound(skyLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 5.0f, 1.65f);
					mWorld.playSound(skyLoc, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 5.0f, 0.65f);
					mWorld.playSound(skyLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 5.0f, 2.0f);
					mWorld.playSound(skyLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 5.0f, 0.5f);

					new PPCircle(Particle.END_ROD, skyLoc, 0.5)
						.count(100)
						.delta(1, 0, 0.5)
						.extra(2)
						.rotateDelta(true)
						.directionalMode(true)
						.spawnAsBoss();

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						new PPCircle(Particle.END_ROD, skyLoc.clone().subtract(0, 8, 0), 0.5)
							.count(85)
							.delta(1, 0, 0.5)
							.extra(1.5)
							.rotateDelta(true)
							.directionalMode(true)
							.spawnAsBoss();
					}, 10);

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						new PPCircle(Particle.END_ROD, skyLoc.clone().subtract(0, 16, 0), 0.5)
							.count(70)
							.delta(1, 0, 0.5)
							.extra(1)
							.rotateDelta(true)
							.directionalMode(true)
							.spawnAsBoss();
					}, 20);

					new PPLine(Particle.END_ROD, skyLoc, starLoc)
						.countPerMeter(5)
						.spawnAsBoss();

					new PPLine(Particle.SOUL_FIRE_FLAME, skyLoc, starLoc)
						.countPerMeter(10)
						.delta(0.1, 0, 0.1)
						.extra(0.1)
						.spawnAsBoss();

					new PPCircle(Particle.REDSTONE, mSpawnLoc.clone().add(0, 0.1, 0), SpellAstralCollapse.START_SIZE + 1)
						.countPerMeter(4)
						.data(new Particle.DustOptions(Color.RED, 2.5f))
						.ringMode(false)
						.spawnAsBoss();

					new PartialParticle(Particle.SQUID_INK, starLoc)
						.count(120)
						.extra(0.8)
						.spawnAsBoss();

					destroyBlocksGenerous(BlockUtils.getBlocksInCylinder(mCenterBlock, SpellAstralCollapse.START_SIZE, 3));

					mBoss.swingMainHand();
					mBoss.setInvulnerable(false);
					mBoss.setAI(true);
					fancyTp(mSpawnLoc.clone().add(VectorUtils.randomHorizontalUnitVector().multiply(10)), 20);

					changePhase(new SpellManager(actives), passives, null);
					forceCastSpell(SpellAstralCollapse.class);
					mPulsarRadiation.onPhase4();

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 20, 1));
	}

	private void showerAndSurge() {
		if (mCelestialPillars.getPillars().isEmpty()) {
			return;
		}
		forceCastSpell(SpellStarShower.class);
		mCollapsingConnection.onStarShower();
		if (mRage >= 20) {
			mStarlightSurge.run();
		}
	}

	private void knockbackPlayers() {
		World world = mBoss.getWorld();
		Location bossLocation = mBoss.getLocation();
		world.playSound(bossLocation, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 1.8f, 1.0f);
		world.playSound(bossLocation, "entity.breeze.wind_burst", SoundCategory.HOSTILE, 1.5f, 0.5f);
		world.playSound(bossLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.0f, 0.9f);

		for (Player player : PlayerUtils.playersInRange(bossLocation, 6, true)) {
			MovementUtils.knockAway(bossLocation, player, 0.4f, 0.7f, false);
		}
		new BukkitRunnable() {
			final Location mLoc = mBoss.getLocation().add(0, 2, 0);
			final double mLowestY = mBoss.getY();
			double mRadius = 0;
			double mYminus = 0.35;

			@Override
			public void run() {
				mRadius += 1;
				new PPCircle(Particle.SPELL_INSTANT, mLoc, mRadius)
					.countPerMeter(3)
					.delta(0.1)
					.spawnAsBoss();

				mLoc.setY(Math.max(mLowestY, mLoc.getY() - mYminus));
				mYminus += 0.02;
				if (mYminus >= 1) {
					mYminus = 1;
				}
				if (mRadius >= 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void absorbPower(Runnable onFinish) {
		changePhase(SpellManager.EMPTY, mPassivesMinis, null);

		mStarlightSurge.cancel();
		mMoonbeams.run();

		List<CelestialPillar> pillars = mCelestialPillars.getPillars();
		int extraSlices = pillars.size() / 2;
		int elites = 1 + pillars.size() / 2;

		mActiveTasks.add(new BukkitRunnable() {
			private int mTicks = 0;
			private final World mBossWorld = mBoss.getWorld();

			@Override
			public void run() {
				for (CelestialPillar pillar : pillars) {
					Location loc = pillar.getLocation();
					if (mTicks % 5 == 0) {
						new PPCircle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.15, 0), 3)
							.count(12)
							.spawnAsBoss();
					}

					if (mTicks == 30) {
						new PPLine(Particle.SMOKE_NORMAL, loc.clone().add(0, 20, 0), loc)
							.count(60)
							.delay(10)
							.delta(0.1)
							.extra(0.01)
							.spawnAsBoss();

						new PPLine(Particle.GUST, loc.clone().add(0, 20, 0), loc)
							.count(20)
							.delay(10)
							.spawnAsBoss();

						new PPLine(Particle.FLAME, loc.clone().add(0, 20, 0), loc)
							.count(40)
							.delay(10)
							.extra(0.1)
							.spawnAsBoss();
					}

					if (mTicks >= 30) {
						mBossWorld.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.1f, 1.5f - mTicks / 20.0f);
						mBossWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.2f - mTicks / 20.0f);
					}

					if (mTicks >= 2 * 20) { // Explode!
						new PPCircle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 0.15, 0), 0.5)
							.count(30)
							.rotateDelta(true)
							.directionalMode(true)
							.delta(0.15, 0, 0)
							.extra(3)
							.spawnAsBoss();
						new PPCircle(Particle.FLAME, loc.clone().add(0, 0.15, 0), 0.5)
							.count(35)
							.rotateDelta(true)
							.directionalMode(true)
							.delta(0.15, 0, 0)
							.extra(3)
							.spawnAsBoss();
						new PPCircle(Particle.SMALL_FLAME, loc.clone().add(0, 0.15, 0), 3)
							.count(20)
							.rotateDelta(true)
							.directionalMode(true)
							.delta(1, 0, 0)
							.extra(-0.15)
							.spawnAsBoss();

						mBossWorld.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.8f, 0.3f);
						mBossWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.2f, 0.2f);
						mBossWorld.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 1.9f, 0.9f);
						mBossWorld.playSound(loc, Sound.ENTITY_BREEZE_DEATH, 1.5f, 0.4f);
						mBossWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 1.8f);

						new PPLine(Particle.SOUL_FIRE_FLAME, pillar.getLocation(), mBoss.getLocation())
							.countPerMeter(4)
							.extra(0.05)
							.delay(10)
							.spawnAsBoss();
					}
				}
				if (mTicks >= 2 * 20) { // Spawn mobs
					mCelestialPillars.cancel();

					mBossWorld.playSound(mSpawnLoc, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 2.5f, 1.8f);
					mBossWorld.playSound(mSpawnLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 2.5f, 0.8f);
					mBossWorld.playSound(mSpawnLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 2.5f, 0.7f);

					new PartialParticle(Particle.EXPLOSION_NORMAL, mSpawnLoc)
						.count(4)
						.delta(0.5)
						.spawnAsBoss();

					mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mMoonbeams.slice(extraSlices, onFinish);

						for (int i = 0; i < elites; i++) {
							Location loc = getRandomArenaLocation(mSpawnLoc, ARENA_INNER_RADIUS);
							new PPLine(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), loc)
								.countPerMeter(5)
								.spawnAsBoss();
							Entity elite = SpellCelestialPillars.ELITE_POOL.spawn(loc);
							if (elite instanceof LivingEntity livingElite) {
								rageBuff(livingElite, mRage, true);
							}
						}
					}, 2 * 20));

					this.cancel();
					return;
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));

	}

	private void clearPillarItems(Location spawnLoc) {
		for (Item item : mBoss.getWorld().getNearbyEntitiesByType(Item.class, spawnLoc, DETECTION_RANGE)) {
			if (isAuroraLoom(item.getItemStack())) {
				item.remove();
			}
		}
	}

	private void fancyTp(Location loc, int delay) {
		fancyTp(mBoss, loc, delay, () -> {
		});
	}

	private void fancyTp(Location loc, int delay, Runnable postTpAction) {
		fancyTp(mBoss, loc, delay, postTpAction);
	}

	public static void fancyTp(LivingEntity boss, Location loc, int delay, Runnable postTpAction) {
		new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(boss), loc.clone().add(0, boss.getHeight() * 0.5, 0))
			.data(new Particle.DustOptions(Color.FUCHSIA, 1.1f + delay / 12.0f))
			.countPerMeter(6)
			.delta(0.1, 0.6, 0.1)
			.delay(delay)
			.spawnAsBoss();

		boss.setAI(false);

		EntityUtils.setDirection(loc.clone().subtract(boss.getLocation()).toVector(), boss);

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			boss.setAI(true);

			new PPPillar(Particle.END_ROD, boss.getLocation(), 2)
				.count(25)
				.spawnAsBoss();

			new PPPillar(Particle.END_ROD, loc, 2)
				.count(25)
				.spawnAsBoss();

			boss.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.7f, 0.8f);
			boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.7f, 0.8f);

			boss.teleport(loc);
			postTpAction.run();
		}, delay);
	}

	private static double getMultiplier(double rage) {
		double multiplier = rage / 100;
		if (rage > 100) {
			return multiplier * Math.log(multiplier) / Math.log(2) + 1;
		}
		return multiplier;
	}

	public static void rageBuff(LivingEntity entity, double rage, boolean elite) {
		double multiplier = getMultiplier(rage);
		EffectManager.getInstance().addEffect(entity, "RageDamage", new PercentDamageDealt(999999999, multiplier / 2));
		EntityUtils.setMaxHealthAndHealth(entity, EntityUtils.getMaxHealth(entity) * (1 + Math.min(multiplier, 1.3) * (elite ? 0.5 : 1)));
	}

	public static void bossRageBuff(LivingEntity entity, double health, double rage, int playerCount) {
		double multiplier = getMultiplier(rage);
		EffectManager.getInstance().addEffect(entity, "RageDamage", new PercentDamageDealt(999999999, multiplier / 2));
		EntityUtils.setMaxHealthAndHealth(entity, health * (1 + multiplier) * BossUtils.healthScalingCoef(playerCount, 0.25, 0.5));
	}

	@Override
	public void init() {
		GlowingManager.startGlowing(mBoss, NamedTextColor.AQUA, -1, GlowingManager.BOSS_SPELL_PRIORITY);
		List<Player> players = playersInRange(mSpawnLoc);

		for (Player player : players) {
			ScoreboardUtils.setScoreboardValue(player, CHARGES_SCORE, 0);
		}
		EntityUtils.setRemoveEntityOnUnload(mBoss);
		bossRageBuff(mBoss, BASE_MAX_HEALTH, mRage, players.size());
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null && !playersInRange(mSpawnLoc).isEmpty()) {
			event.setCancelled(true);
			event.setReviveHealth(100);
			deathCutscene();
		}
		getPassives().forEach(Spell::cancel);
		changePhase(new SpellManager(List.of()), List.of(), null);
		mPulsarRadiation.cancel();
	}

	private void deathCutscene() {
		World world = mBoss.getWorld();
		Location bossLoc = LocationUtils.getHalfHeightLocation(mBoss);

		world.playSound(bossLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 5.0f, 0.6f);
		world.playSound(bossLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.HOSTILE, 5.0f, 0.1f);
		world.playSound(bossLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 5.0f, 0.8f);

		new PartialParticle(Particle.EXPLOSION_LARGE, bossLoc)
			.minimumCount(1)
			.extra(3)
			.spawnAsBoss();

		new PPSpiral(Particle.CLOUD, bossLoc, 12)
			.countPerBlockPerCurve(3)
			.curveAngle(500)
			.extra(0.2)
			.ticks(10)
			.spawnAsBoss();

		List<Player> players = playersInRange(mSpawnLoc);
		BossUtils.endBossFightEffects(mBoss, players, 12 * 20, true, false);
		players.forEach(player -> {
			player.setVelocity(new Vector(0, 0.8, 0));
			player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 8 * 20, -1));
		});
		dialogue(2 * 20, List.of(
			Dialogue.aurora("I can feel her, she is weakened."),
			Dialogue.aurora("We can end this... We must open... the Veil..."),
			Dialogue.aurora("Whatever we will become, I do not know. But the Stars ᴡɪʟʟ ᴅɪᴇ ᴡɪᴛʜ ᴜꜱ.")
		), () -> {
			mBoss.remove();
			mEndLoc.getBlock().setBlockData(Material.REDSTONE_BLOCK.createBlockData());

			new PartialParticle(Particle.EXPLOSION_NORMAL, bossLoc)
				.count(45)
				.extra(1)
				.spawnAsBoss();

			world.playSound(bossLoc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.HOSTILE, 2.5f, 1.1f);
			world.playSound(bossLoc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 2.5f, 0.1f);
			world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.9f, 1.0f);
			world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 2.5f, 0.1f);
			world.playSound(bossLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 2.5f, 1.25f);

			world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.3f);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				world.playSound(bossLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 2.5f, 0.8f);
				world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.9f, 0.8f);
			}, 3);

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				world.playSound(bossLoc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 2.5f, 0.5f);
				world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.7f, 0.5f);
			}, 6);

			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				world.playSound(mSpawnLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 5.0f, 0.7f);
				playersInRange(mSpawnLoc, true).forEach(player -> {
					MessagingUtils.sendBoldTitle(
						player,
						Component.text("VICTORY", NamedTextColor.WHITE),
						Component.text("Aurora Lunacrestum, the Starspeaker", NamedTextColor.LIGHT_PURPLE)
					);
					mPlugin.mEffectManager.clearEffects(player, SpellStarlightSurge.VULNERABILITY_SOURCE);
				});

				mAdvancements.bossDeath(mRage);
			}, 20);
		}, 3 * 20);
	}

	@Override
	public void unload() {
		super.unload();

		clearPillarItems(mSpawnLoc);

		playersInRange(mSpawnLoc, true).forEach(player -> {
			ScoreboardUtils.setScoreboardValue(player, CHARGES_SCORE, 0);
		});

		mActiveTasks.forEach(BukkitTask::cancel);
		mAuroraMinis.cancel();
		mPulsarRadiation.cancel();
		mCelestialPillars.cancel();
		mStarlightSurge.cancel();
		mSupernova.cancel();
		mMoonbeams.cancel();
		mCollapsingConnection.cancel();
		getPassives().forEach(Spell::cancel); // cancel silly passives too!!!!

		EntityUtils.getNearbyMobs(mSpawnLoc, DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE, EntityUtils::isHostileMob).forEach(Entity::remove);
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mBrokenBlocks, Material.AIR);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		if (mRage < 180) {
			return;
		}
		Class<? extends Spell> castSpell = event.getSpell().getClass();
		List<Spell> otherSpells = mActiveSpells.getSpells().stream()
			.filter(spell -> !spell.onlyForceCasted() && spell.canRun() && !castSpell.isInstance(spell))
			.toList();
		if (otherSpells.isEmpty()) {
			return;
		}
		Spell otherSpell = FastUtils.getRandomElement(otherSpells);
		otherSpell.run();
		setActiveCooldown(Math.max(getActiveCooldown(), otherSpell.cooldownTicks()));
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mStarShower.isRunning()) {
			Location location = mBoss.getLocation();

			mBoss.getWorld().playSound(location, Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.5f, 1.25f);
			new PartialParticle(Particle.FIREWORKS_SPARK, location)
				.count(30)
				.extra(0.2)
				.spawnAsBoss();

			event.updateFinalMultiplier(0.1);
		}
	}

	@Override
	public void nearbyBlockPlace(BlockPlaceEvent event) {
		mAdvancements.addDestroyedBlocks(-1);
	}

	@Override
	public boolean hasNearbyBlockPlaceTrigger() {
		return true;
	}

	@Override
	public void nearbyBlockBreak(BlockBreakEvent event) {
		if (mCelestialPillars.isAnyPillar(event.getBlock().getLocation())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(Component.text("Your tools seem to have no effect on the pillar, perhaps a powerful falling star would work.", NamedTextColor.GRAY));
		}
	}

	@Override
	public boolean hasNearbyBlockBreakTrigger() {
		return true;
	}

	private void startBoss(List<Player> players, List<Spell> activeSpells, List<Spell> passiveSpells, BossBarManager bossBarManager) {
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		constructBoss(new SpellManager(activeSpells), passiveSpells, DETECTION_RANGE, bossBarManager, 6 * 20, 1, true);

		dialogue(2 * 20, List.of(
			Dialogue.of("ꜱɪꜱᴛᴇʀꜱ, ᴡᴇ ᴀʀᴇ ꜱᴏ ᴄʟᴏꜱᴇ ᴛᴏ ʀᴇᴜɴɪᴛɪɴɢ.", "[Aurora?]", DELFIA_COLOR),
			Dialogue.of("ɪᴛ ɪꜱ ᴛɪᴍᴇ ᴛᴏ ᴅᴇꜱᴛʀᴏʏ ᴛʜɪꜱ ᴠᴇɪʟ ᴀɴᴅ ꜰᴇᴀꜱᴛ!", "[Aurora?]", AURORA_COLOR)
		), () -> {
			mBoss.setAI(true);
			mBoss.setInvulnerable(false);

			players.forEach(player -> {
				player.showTitle(Title.title(
					Component.text("Aurora Lunacrestum", NamedTextColor.WHITE, TextDecoration.BOLD),
					Component.text("The Starspeaker", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
				));
				player.playSound(mSpawnLoc, Sound.ENTITY_WITHER_SPAWN, 2.5f, 0.1f);
			});
			mCelestialPillars.run();
			if (mRage >= 120) {
				mCollapsingConnection.run();
			}
		}, 20);
	}

	public static List<Player> playersInRange(Location bossLoc) {
		return playersInRange(bossLoc, false);
	}

	public static List<Player> playersInRange(Location bossLoc, boolean includeDead) {
		List<Player> players = new ArrayList<>();
		PlayerUtils.playersInRange(bossLoc, DETECTION_RANGE, true).forEach(player -> {
			if (includeDead || player.getScoreboardTags().contains(ALIVE_TAG)) {
				players.add(player);
			}
		});
		return players;
	}

	public static boolean isAlive(Player player) {
		return player.getScoreboardTags().contains(ALIVE_TAG);
	}

	public static void onPickupPillarMatter(Player player) {
		ScoreboardUtils.addScore(player, CHARGES_SCORE, 1);
		Location loc = player.getLocation();
		player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.8f, 1.5f);
		player.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.BLOCKS, 1.8f, 1.2f);

		player.sendMessage(Component.text("Your Loamskattar Catalyst absorbs the matter from the broken Pillar.", NamedTextColor.AQUA, TextDecoration.ITALIC));
		player.updateInventory();
	}

	public static void useStoredCharges(Player player) {
		ScoreboardUtils.addScore(player, CHARGES_SCORE, -1);
	}

	public static boolean hasStoredCharges(Player player) {
		return getStoredCharges(player) > 0;
	}

	public static int getStoredCharges(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, CHARGES_SCORE).orElse(0);
	}

	private void moonlightCdr(Player player) {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 1.6f, 2.0f);
		world.playSound(bossLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 1.6f, 1.5f);

		ParticleUtils.launchOrb(VectorUtils.randomHorizontalUnitVector().setY(0.6),
			player.getLocation(),
			mBoss,
			mBoss,
			10 * 20,
			bossLoc.add(0, 0.6, 0),
			new Particle.DustOptions(Color.fromRGB(0xb2fffc), 1.4f),
			entity -> world.playSound(bossLoc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 0.4f, 2.0f)
		);
		if (mCdrWarned.add(player)) { // first time seeing this
			player.sendMessage(MessagingUtils.fromMiniMessage(String.format(
				"<color:gray>As Aurora's Moonlight Slash hits you, she reduces her <color:white>active spell cooldowns</color> by %s seconds!</color>", COOLDOWN_REDUCTION * (mRage >= 200 ? 2 : 1) / 20
			)));
		}

		reduceActiveCooldown((mRage >= 200 ? 2 : 1) * COOLDOWN_REDUCTION);
		getActiveSpells().forEach(spell -> {
			if (spell instanceof CooldownReducible s) {
				s.reduceCooldown((mRage >= 200 ? 2 : 1) * COOLDOWN_REDUCTION);
			}
		});
	}

	public static double getSurfaceY(Location loc, Location center) {
		double distance = LocationUtils.xzDistance(loc, center);
		double y = center.getY();
		if (distance <= ARENA_INNER_RADIUS) {
			return y;
		} else if (distance <= ARENA_SEMI_OUTER_RADIUS) {
			return y + 1;
		}
		return y + 2;
	}

	public static Location withSurfaceY(Location loc, Location center) {
		Location clone = loc.clone();
		clone.setY(getSurfaceY(clone, center));
		return clone;
	}

	public void usedLoamskattarCatalyst() {
		mAdvancements.usedLoamskattarCatalyst();
	}

	public void repairBlocks(Collection<Block> blocks) {
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(blocks, Material.AIR);
		mAdvancements.addDestroyedBlocks(-blocks.stream().filter(Block::isSolid).count());
	}

	public void destroyBlocksHarsh(Collection<Block> blocks) {
		doBlockBreak(blocks, 5, 10);
	}

	public void destroyBlocks(Collection<Block> blocks) {
		doBlockBreak(blocks, 10, 15);
	}

	public void destroyBlocksGenerous(Collection<Block> blocks) {
		doBlockBreak(blocks, 30, 40);
	}

	private void doBlockBreak(Collection<Block> blocks, int minTime, int maxTime) {
		long count = 0;
		for (Block block : blocks) {
			Location bLoc = block.getLocation().add(0.5, 0.5, 0.5);
			BlockData blockData = block.getBlockData();
			Material type = blockData.getMaterial();
			boolean withinProtectedRadius = bLoc.distanceSquared(mCenterBlock) > PROTECTED_RADIUS * PROTECTED_RADIUS;

			mCelestialPillars.checkAndDestroyPillars(bLoc);

			if (isBreakableMaterial(type) && (mLastPhase || withinProtectedRadius)) {
				int warnDuration = FastUtils.randomIntInRange(minTime, maxTime);
				Runnable breakBlock = () -> {
					new PartialParticle(Particle.BLOCK_CRACK, bLoc)
						.count(5)
						.distanceFalloff(ARENA_RADIUS)
						.data(blockData)
						.delta(0.3)
						.extra(0.5)
						.spawnAsBoss();
					TemporaryBlockChangeManager.INSTANCE.changeBlock(block, Material.AIR, 99999999);
					mBrokenBlocks.add(block);
				};
				if (warnDuration > 0) {
					TemporaryBlockChangeManager.INSTANCE.changeBlock(block, blockData instanceof Slab ? Material.MANGROVE_SLAB : Material.STRIPPED_MANGROVE_WOOD, 2 * 20);
					Bukkit.getScheduler().runTaskLater(mPlugin, breakBlock, warnDuration);
				} else {
					breakBlock.run();
				}

				count++;
			}
		}
		mAdvancements.addDestroyedBlocks(count);
	}

	public static boolean isBreakableMaterial(Material type) {
		return type != Material.AIR &&
			type != Material.MANGROVE_SLAB &&
			type != Material.STRIPPED_MANGROVE_WOOD &&
			type != Material.DIORITE_SLAB &&
			type != Material.WHITE_STAINED_GLASS &&
			type != Material.LIGHT;

	}

	public static Location getRandomArenaLocation(Location center, double excludeRadius) {
		Location lowerCenter = center.clone().subtract(0, 1, 0);
		Location randomLoc = LocationUtils.randomSafeLocationInDonut(
			center,
			excludeRadius,
			ARENA_RADIUS - 1,
			location -> withSurfaceY(location, lowerCenter).getBlock().isSolid()
		);
		return withSurfaceY(randomLoc, center);
	}

	private void dialogue(int delay, List<Dialogue> messages, Runnable runAfterDialogue, int runnableDelay) {
		mActiveTasks.add(new BukkitRunnable() {
			int mSafety = 0;
			int mIndex = 0;

			@Override
			public void run() {
				// safety check
				if (mSafety >= messages.size() + 1 || mIndex >= messages.size()) {
					this.cancel();
					return;
				}
				mSafety++;

				// send message
				Dialogue dialogue = messages.get(mIndex);
				dialogue(dialogue.message, dialogue.speaker, dialogue.textColor);

				mIndex++;
				if (mIndex >= messages.size()) {
					this.cancel();
					mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, runAfterDialogue, runnableDelay));
				}
			}
		}.runTaskTimer(mPlugin, 0, delay));
	}

	public void dialogue(String message, String speaker, TextColor textColor) {
		playersInRange(mSpawnLoc, true).forEach(player -> player.sendMessage(formatMessage(message, speaker, textColor)));
	}

	public void dialogue(int delay, List<Dialogue> messages) {
		dialogue(delay, messages, () -> {
		}, 0);
	}

	public static TextComponent formatMessage(String message, String speaker, TextColor textColor) {
		return Component.text(speaker + " ", NamedTextColor.GOLD).append(Component.text(message, textColor));
	}

	public static boolean isAuroraLoom(ItemStack itemStack) {
		return ItemUtils.getPlainName(itemStack).equals(AURORA_LOOM_NAME);
	}

}
