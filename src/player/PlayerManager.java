package de.stylextv.gs.player;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.stylextv.gs.image.ImageGenerator;
import de.stylextv.gs.main.Vars;
import de.stylextv.gs.world.WorldUtil;

public class PlayerManager {
	
	public static double[] matrix;
	public static int n=8;
	private static Point[] directions=new Point[]{new Point(1, 0),new Point(0, 1),new Point(-1, 0),new Point(0, -1)};
	
	private static ConcurrentHashMap<Player, Order> playerTasks=new ConcurrentHashMap<Player, Order>();
	
	public static void init() {
		matrix = new double[] {
				0,48,12,60,3,51,15,63,
				32,16,44,28,35,19,47,31,
				8,56,4,52,11,59,7,55,
				40,24,36,20,43,27,39,23,
				2,50,14,62,1,49,13,61,
				34,18,46,30,33,17,45,29,
				10,58,6,54,9,57,5,53,
				42,26,38,22,41,25,37,21
		};
		for(int j=0; j<matrix.length; j++) {
			matrix[j]=(matrix[j]+1)/(double)(n*n) - 0.5;
		}
	}
	
	public static void startPlacingPhase(Player p, Order order) {
		playerTasks.put(p, order);
		p.sendMessage(Vars.PREFIX+"Your sign was created �asuccessfully�7. Please click with the left mouse button on one of the �estone blocks�7 to place it.");
	}
	public static void cancelPlacingPhase(Player p) {
		Order o=playerTasks.remove(p);
		if(o!=null) p.sendMessage(Vars.PREFIX+"The placement process has been �ccanceled�7.");
		else p.sendMessage(Vars.PREFIX+"You are currently not in a placement �cprocess�7.");
	}
	
	public static void onPlayerQuit(PlayerQuitEvent e) {
		WorldUtil.removeAllDrewEntries(e.getPlayer());
	}
	
	public static void onPlayerInteract(PlayerInteractEvent e) {
		if(e.getAction()==Action.LEFT_CLICK_BLOCK) {
			Block b=e.getClickedBlock();
			if(b.getType().equals(Material.STONE)) {
				Player p=e.getPlayer();
				Order order=playerTasks.get(p);
				if(order!=null) {
					Location top=b.getLocation();
					Location bottom=b.getLocation().clone();
					int maxI=0;
					while(top.getBlock().getRelative(BlockFace.UP).getType().equals(Material.STONE)) {
						top.add(0,1,0);
						maxI++;
						if(maxI>10) break;
					}
					maxI=0;
					while(bottom.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.STONE)) {
						bottom.add(0,-1,0);
						maxI++;
						if(maxI>10) break;
					}
					if(top.getBlock().getRelative(BlockFace.NORTH).getType().equals(Material.STONE)||top.getBlock().getRelative(BlockFace.SOUTH).getType().equals(Material.STONE)) {
						maxI=0;
						while(top.getBlock().getRelative(BlockFace.NORTH).getType().equals(Material.STONE)) {
							top.add(0,0,-1);
							maxI++;
							if(maxI>10) break;
						}
						maxI=0;
						while(bottom.getBlock().getRelative(BlockFace.SOUTH).getType().equals(Material.STONE)) {
							bottom.add(0,0,1);
							maxI++;
							if(maxI>10) break;
						}
					} else if(top.getBlock().getRelative(BlockFace.EAST).getType().equals(Material.STONE)||top.getBlock().getRelative(BlockFace.WEST).getType().equals(Material.STONE)) {
						maxI=0;
						while(top.getBlock().getRelative(BlockFace.EAST).getType().equals(Material.STONE)) {
							top.add(1,0,0);
							maxI++;
							if(maxI>10) break;
						}
						maxI=0;
						while(bottom.getBlock().getRelative(BlockFace.WEST).getType().equals(Material.STONE)) {
							bottom.add(-1,0,0);
							maxI++;
							if(maxI>10) break;
						}
					}
					boolean placed=false;
					for(Point dir:directions) {
						boolean valid=true;
						if(dir.x!=0) {
							valid=top.getBlockX()==bottom.getBlockX()||(top.getBlockZ()==bottom.getBlockZ()&&top.getBlockX()==bottom.getBlockX());
						}
						
						if(valid) {
							boolean save=true;
							for(int x=bottom.getBlockX(); x<=top.getBlockX(); x++) {
								for(int y=bottom.getBlockY(); y<=top.getBlockY(); y++) {
									if(!save) break;
									for(int z=top.getBlockZ(); z<=top.getBlockZ(); z++) {
										Block block=top.getWorld().getBlockAt(x+dir.x, y, z+dir.y);
										if(!(block.getType().isSolid()&&!block.getType().equals(Material.STONE)&&top.getWorld().getBlockAt(x, y, z).getType().equals(Material.STONE))) {
											save=false;
											break;
										}
									}
								}
							}
							if(save) {
								int imgHeight=top.getBlockY()-bottom.getBlockY()+1;
								if(dir.x!=0) {
									int minZ;
									int maxZ;
									BlockFace face;
									minZ=Math.min(top.getBlockZ(),bottom.getBlockZ());
									maxZ=Math.max(top.getBlockZ(),bottom.getBlockZ());
									if(dir.x==-1) {
										face=BlockFace.EAST;
									} else {
										face=BlockFace.WEST;
									}
									int imgWidth=maxZ-minZ+1;
									BufferedImage image=ImageGenerator.generate(order,imgWidth,imgHeight);
									
									for(int z=minZ; z<=maxZ; z++) {
										for(int y=top.getBlockY(); y>=bottom.getBlockY(); y--) {
											Location loc=new Location(top.getWorld(), top.getBlockX(), y, z);
											loc.getBlock().setType(Material.AIR);
											int imgY=top.getBlockY()-y;
											int imgX;
											if(dir.x==-1) imgX=maxZ-z;
											else imgX=z-minZ;
											WorldUtil.spawnItemFrame(top.getWorld(), loc, image.getSubimage(imgX*128, imgY*128, 128, 128), face);
										}
									}
								} else {
									int minX;
									int maxX;
									BlockFace face;
									maxX=Math.max(top.getBlockX(),bottom.getBlockX());
									minX=Math.min(top.getBlockX(),bottom.getBlockX());
									if(dir.y==-1) {
										face=BlockFace.SOUTH;
									} else {
										face=BlockFace.NORTH;
									}
									int imgWidth=maxX-minX+1;
									BufferedImage image=ImageGenerator.generate(order,imgWidth,imgHeight);
									
									for(int x=minX; x<=maxX; x++) {
										for(int y=top.getBlockY(); y>=bottom.getBlockY(); y--) {
											Location loc=new Location(top.getWorld(), x, y, top.getBlockZ());
											loc.getBlock().setType(Material.AIR);
											int imgY=top.getBlockY()-y;
											int imgX;
											if(dir.y==-1) imgX=x-minX;
											else imgX=maxX-x;
											WorldUtil.spawnItemFrame(top.getWorld(), loc, image.getSubimage(imgX*128, imgY*128, 128, 128), face);
										}
									}
								}
								
								placed=true;
								p.sendMessage(Vars.PREFIX+"Your sign has been �aplaced�7 successfully.");
								playerTasks.remove(p);
								break;
							}
						}
					}
					if(!placed) p.sendMessage(Vars.PREFIX+"There must be �csolid �7blocks to hang a sign.");
					e.setCancelled(true);
				}
			}
		}
	}
	
}
