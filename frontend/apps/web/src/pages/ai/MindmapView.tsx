import { useEffect, useRef } from 'react'
import { Markmap } from 'markmap-view'
import type { MindmapNode } from '@edu/api'

interface PureNode {
  content: string
  children: PureNode[]
}

/** 将后端 {title|content, children} 树转换为 Markmap 的 IPureNode */
function toPureNode(node: MindmapNode): PureNode {
  return {
    content: node.content ?? node.title ?? '',
    children: (node.children ?? []).map(toPureNode),
  }
}

/**
 * Markmap.js 思维导图渲染（Web Only：依赖 SVG/DOM）。
 */
export function MindmapView({ data }: { data: MindmapNode }) {
  const svgRef = useRef<SVGSVGElement>(null)
  const mmRef = useRef<Markmap | null>(null)

  useEffect(() => {
    if (!svgRef.current) return
    if (!mmRef.current) {
      mmRef.current = Markmap.create(svgRef.current)
    }
    mmRef.current.setData(toPureNode(data))
    void mmRef.current.fit()
  }, [data])

  useEffect(() => {
    return () => {
      mmRef.current?.destroy()
      mmRef.current = null
    }
  }, [])

  return <svg ref={svgRef} className="w-full h-[480px]" />
}
