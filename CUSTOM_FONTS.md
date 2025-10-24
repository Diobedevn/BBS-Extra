# Custom TTF Font Support

The BBS mod supports custom TrueType Font (TTF) files for labels and subtitles. However, due to Minecraft's font system architecture, TTF fonts require special setup.

## Current Support

✅ **Minecraft built-in fonts**: `minecraft:default`, `minecraft:alt`, `minecraft:uniform` - Work perfectly!  
⚠️ **Custom TTF fonts**: Currently detected but require resource pack setup to actually render.

## How Minecraft Fonts Work

Minecraft's font system uses a resource pack structure:
1. Font files (.ttf) go in `assets/<namespace>/font/` directory
2. Font definitions (.json) go in `assets/<namespace>/font/` directory  
3. The JSON file tells Minecraft how to load and render the font

## Setting Up Custom TTF Fonts (Workaround)

Since dynamic font registration is complex, here's how to use custom TTF fonts:

### Option 1: Create a Resource Pack

1. Create a resource pack structure:
   ```
   my_fonts_pack/
   ├── pack.mcmeta
   └── assets/
       └── bbs/
           └── font/
               ├── my_font.ttf
               └── my_font.json
   ```

2. Create `pack.mcmeta`:
   ```json
   {
     "pack": {
       "pack_format": 15,
       "description": "Custom fonts for BBS"
     }
   }
   ```

3. Create `my_font.json` (font definition):
   ```json
   {
     "providers": [
       {
         "type": "ttf",
         "file": "bbs:font/my_font.ttf",
         "shift": [0.0, 0.0],
         "size": 11.0,
         "oversample": 2.0
       }
     ]
   }
   ```

4. Reference it in BBS as `bbs:my_font` instead of `assets:font/my_font.ttf`

### Option 2: Use Minecraft's Built-in Fonts

The simplest solution is to use Minecraft's 3 built-in fonts which work perfectly:
- `minecraft:default` - Standard Minecraft font
- `minecraft:alt` - Alternative/Unicode font
- `minecraft:uniform` - Monospaced font

## Future Plans

We're exploring ways to:
- Auto-generate font JSON definitions from TTF files
- Dynamically register fonts at runtime
- Provide a simpler workflow for custom fonts

For now, the three Minecraft built-in fonts provide good variety and work reliably!

## Technical Notes

Minecraft's `FontManager` class doesn't expose public APIs for dynamically loading TTF files. The fonts are loaded during resource pack initialization, which happens at game startup and resource reload. Dynamic font registration would require:
- Injecting into the resource loading pipeline
- Generating font JSON definitions programmatically
- Triggering font reload after registration

This is technically possible but adds significant complexity.
